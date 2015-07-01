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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

/**
 * A list which forwards all its method calls to another list. Subclasses should
 * override one or more methods to modify the behavior of the backing list as
 * desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p>This class does not implement {@link java.util.RandomAccess}. If the
 * delegate supports random access, the {@code ForwadingList} subclass should
 * implement the {@code RandomAccess} interface.
 *
 * @author Mike Bostock
 */
@SuppressWarnings("nullness:generic.argument")
@GwtCompatible
    public abstract class ForwardingList<E extends /*@Nullable*/ Object> extends ForwardingCollection<E>
    implements List<E> {

  @Override protected abstract List<E> delegate();

  @Override
public void add(int index, E element) {
    delegate().add(index, element);
  }

  @Override
public boolean addAll(int index, Collection<? extends E> elements) {
    return delegate().addAll(index, elements);
  }

  @Override
public E get(int index) {
    return delegate().get(index);
  }

  @Override
@Pure public int indexOf(/*@Nullable*/ Object element) {
    return delegate().indexOf(element);
  }

  @Override
@Pure public int lastIndexOf(/*@Nullable*/ Object element) {
    return delegate().lastIndexOf(element);
  }

  @Override
public ListIterator<E> listIterator() {
    return delegate().listIterator();
  }

  @Override
public ListIterator<E> listIterator(int index) {
    return delegate().listIterator(index);
  }

  @Override
public E remove(int index) {
    return delegate().remove(index);
  }

  @Override
public E set(int index, E element) {
    return delegate().set(index, element);
  }

  @Override
@GwtIncompatible("List.subList")
  @SideEffectFree public List<E> subList(int fromIndex, int toIndex) {
    return Platform.subList(delegate(), fromIndex, toIndex);
  }

  @Pure @Override public boolean equals(@Nullable Object object) {
    return object == this || delegate().equals(object);
  }

  @Pure @Override public int hashCode() {
    return delegate().hashCode();
  }
}
