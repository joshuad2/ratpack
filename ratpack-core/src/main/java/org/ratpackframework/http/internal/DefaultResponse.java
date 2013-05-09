/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ratpackframework.http.internal;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import org.ratpackframework.file.internal.FileHttpTransmitter;
import org.ratpackframework.http.MediaType;
import org.ratpackframework.http.Response;
import org.ratpackframework.util.IoUtils;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DefaultResponse implements Response {

  private final FullHttpResponse response;
  private final Channel channel;

  private Set<Cookie> cookies;

  public DefaultResponse(FullHttpResponse response, Channel channel) {
    this.response = response;
    this.channel = channel;
  }

  @Override
  public Status getStatus() {
    return new Status() {
      @Override
      public int getCode() {
        return response.getStatus().code();
      }

      @Override
      public String getMessage() {
        return response.getStatus().reasonPhrase();
      }
    };
  }

  @Override
  public Response status(int code) {
    response.setStatus(HttpResponseStatus.valueOf(code));
    return this;
  }

  @Override
  public Response status(int code, String message) {
    response.setStatus(new HttpResponseStatus(code, message));
    return this;
  }

  @Override
  public void send() {
    commit();
  }

  protected Response contentType(String contentType) {
    setHeader(HttpHeaders.Names.CONTENT_TYPE, new MediaType(contentType, "utf8").toString());
    return this;
  }

  @Override
  public void send(String contentType, String str) {
    contentType(contentType);
    response.content().writeBytes(IoUtils.utf8Buffer(str));
    commit();
  }

  @Override
  public void send(String text) {
    send("text/plain", text);
  }

  @Override
  public void send(String contentType, ByteBuf buffer) {
    contentType(contentType);
    response.content().writeBytes(buffer);
    commit();
  }

  @Override
  public void sendFile(String contentType, File file) {
    contentType(contentType);
    setCookieHeader();
    new FileHttpTransmitter().transmit(file, response, channel);
  }

  public void redirect(String location) {
    response.setStatus(HttpResponseStatus.FOUND);
    setHeader(HttpHeaders.Names.LOCATION, location);
    commit();
  }

  @Override
  public void redirect(int code, String location) {
    status(code);
    setHeader(HttpHeaders.Names.LOCATION, location);
    commit();
  }

  @Override
  public String getHeader(String name) {
    return response.headers().get(name);
  }

  @Override
  public List<String> getHeaders(String name) {
    return response.headers().getAll(name);
  }

  @Override
  public boolean containsHeader(String name) {
    return response.headers().contains(name);
  }

  @Override
  public Set<String> getHeaderNames() {
    return response.headers().names();
  }

  @Override
  public void addHeader(String name, Object value) {
    response.headers().add(name, value);
  }

  @Override
  public void setHeader(String name, Object value) {
    response.headers().set(name, value);
  }

  @Override
  public void setHeader(String name, Iterable<?> values) {
    response.headers().set(name, values);
  }

  @Override
  public void removeHeader(String name) {
    response.headers().remove(name);
  }

  @Override
  public void clearHeaders() {
    response.headers().clear();
  }

  @Override
  public Set<Cookie> getCookies() {
    if (cookies == null) {
      cookies = new HashSet<>();
    }
    return cookies;
  }

  @Override
  public Cookie cookie(String name, String value) {
    Cookie cookie = new DefaultCookie(name, value);
    getCookies().add(cookie);
    return cookie;
  }

  @Override
  public Cookie expireCookie(String name) {
    Cookie cookie = cookie(name, "");
    cookie.setMaxAge(0);
    return cookie;
  }

  private void setCookieHeader() {
    if (cookies != null && !cookies.isEmpty()) {
      for (Cookie cookie : cookies) {
        response.headers().add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(cookie));
      }
    }
  }

  private void commit() {
    setCookieHeader();
    if (channel.isOpen()) {
      ChannelFuture future = channel.write(response);
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }


}