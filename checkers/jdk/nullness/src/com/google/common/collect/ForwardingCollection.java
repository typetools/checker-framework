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

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.Iterator;

import checkers.nullness.quals.*;

/**
 * A collection which forwards all its method calls to another collection.
 * Subclasses should override one or more methods to modify the behavior of
 * the backing collection as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * @see ForwardingObject
 * @author Kevin Bourrillion
 */
@GwtCompatible
@SuppressWarnings("nullness:generic.argument")
public abstract class ForwardingCollection<E extends /*@Nullable*/ Object> extends ForwardingObject
    implements Collection<E> {

  @Override protected abstract Collection<E> delegate();

  public Iterator<E> iterator() {
    return delegate().iterator();
  }

  public int size() {
    return delegate().size();
  }

  public boolean removeAll(Collection<?> collection) {
    return delegate().removeAll(collection);
  }

  public boolean isEmpty() {
    return delegate().isEmpty();
  }

  public boolean contains(/*@Nullable*/ Object object) {
    return delegate().contains(object);
  }

  @SuppressWarnings("nullness")
  // Suppressed due to annotations of toArray
  public /*@Nullable*/ Object[] toArray() {
    return delegate().toArray();
  }

  @SuppressWarnings("nullness")
  public <T extends /*@Nullable*/ Object> T[] toArray(T[] array) {
    return delegate().toArray(array);
  }

  public boolean add(E element) {
    return delegate().add(element);
  }

  public boolean remove(/*@Nullable*/ Object object) {
    return delegate().remove(object);
  }

  @SuppressWarnings("nullness")
  // Suppressed due to the containsAll method in Collection
  public boolean containsAll(Collection<? extends /*@Nullable*/ Object> collection) {
    return delegate().containsAll(collection);
  }

  public boolean addAll(Collection<? extends E> collection) {
    return delegate().addAll(collection);
  }

  @SuppressWarnings("nullness")
  // Suppressed due to the containsAll method in Collection
  public boolean retainAll(Collection<? extends /*@Nullable*/ Object> collection) {
    return delegate().retainAll(collection);
  }

  public void clear() {
    delegate().clear();
  }
}
