/*
 * Copyright 2014 the original author or authors.
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

package ratpack.stream.internal;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Iterator;

public class IterablePublisher<T> implements Publisher<T> {

  private final Iterable<T> iterable;

  public IterablePublisher(Iterable<T> iterable) {
    this.iterable = iterable;
  }

  @Override
  public void subscribe(final Subscriber<? super T> subscriber) {
    final Iterator<T> iterator = iterable.iterator();
    new Subscription(subscriber, iterator);
  }

  private class Subscription extends SubscriptionSupport<T> {
    private final Iterator<T> iterator;

    public Subscription(Subscriber<? super T> subscriber, Iterator<T> iterator) {
      super(subscriber);
      this.iterator = iterator;
      start();
    }

    @Override
    protected void doRequest(long n) {
      for (int i = 0; i < n; ++i) {
        if (iterator.hasNext()) {
          T next;
          try {
            next = iterator.next();
          } catch (Exception e) {
            onError(e);
            return;
          }
          onNext(next);
        } else {
          break;
        }
      }

      if (!iterator.hasNext()) {
        onComplete();
      }
    }

  }
}
