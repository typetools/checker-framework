/*
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import org.checkerframework.dataflow.qual.SideEffectFree;

import java.util.Comparator;
import java.util.SortedSet;

import com.google.common.annotations.GwtCompatible;

/**
 * A sorted set which forwards all its method calls to another sorted set.
 * Subclasses should override one or more methods to modify the behavior of the
 * backing sorted set as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * @see ForwardingObject
 * @author Mike Bostock
 */
@GwtCompatible
@SuppressWarnings("nullness:generic.argument")
  public abstract class ForwardingSortedSet<E extends /*@Nullable*/ Object> extends ForwardingSet<E>
    implements SortedSet<E> {

  @Override protected abstract SortedSet<E> delegate();

  @Override
@SideEffectFree public Comparator<? super E> comparator() {
    return delegate().comparator();
  }

  @Override
@SideEffectFree public E first() {
    return delegate().first();
  }

  @Override
@SideEffectFree public SortedSet<E> headSet(E toElement) {
    return delegate().headSet(toElement);
  }

  @Override
@SideEffectFree public E last() {
    return delegate().last();
  }

  @Override
@SideEffectFree public SortedSet<E> subSet(E fromElement, E toElement) {
    return delegate().subSet(fromElement, toElement);
  }

  @Override
@SideEffectFree public SortedSet<E> tailSet(E fromElement) {
    return delegate().tailSet(fromElement);
  }
}
