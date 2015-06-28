/*
 * Copyright (C) 2009 Google Inc.
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

import static com.google.common.base.Preconditions.checkNotNull;

import org.checkerframework.checker.nullness.qual.Nullable;
//import javax.annotation.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;

/**
 * Implementation of {@link ImmutableList} with exactly one element.
 *
 * @author Hayward Chan
 */
@GwtCompatible(serializable = true)
    @SuppressWarnings({"serial","nullness:generic.argument"}) // uses writeReplace(), not default serialization
final class SingletonImmutableList<E> extends ImmutableList<E> {
  final transient E element;

  SingletonImmutableList(E element) {
    this.element = checkNotNull(element);
  }

  @Override
public E get(int index) {
    Preconditions.checkElementIndex(index, 1);
    return element;
  }

  @Pure @Override public int indexOf(@Nullable Object object) {
    return element.equals(object) ? 0 : -1;
  }

  @Override public UnmodifiableIterator<E> iterator() {
    return Iterators.singletonIterator(element);
  }

  @Pure @Override public int lastIndexOf(@Nullable Object object) {
    return element.equals(object) ? 0 : -1;
  }

  @Override
public ListIterator<E> listIterator() {
    return listIterator(0);
  }

  @Override
public ListIterator<E> listIterator(final int start) {
    // suboptimal but not worth optimizing.
    return Collections.singletonList(element).listIterator(start);
  }

  @Override
@Pure public int size() {
    return 1;
  }

  @SideEffectFree @Override public ImmutableList<E> subList(int fromIndex, int toIndex) {
    Preconditions.checkPositionIndexes(fromIndex, toIndex, 1);
    return (fromIndex == toIndex) ? ImmutableList.<E>of() : this;
  }

  @Pure @Override public boolean contains(@Nullable Object object) {
    return element.equals(object);
  }

  @Pure @Override public boolean equals(/*@Nullable*/ Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof List) {
      List<?> that = (List<?>) object;
      return that.size() == 1 && element.equals(that.get(0));
    }
    return false;
  }

  @Pure @Override public int hashCode() {
    // not caching hash code since it could change if the element is mutable
    // in a way that modifies its hash code.
    return 31 + element.hashCode();
  }

  @Pure @Override public boolean isEmpty() {
    return false;
  }

  @Override public Object[] toArray() {
    return new Object[] { element };
  }

  @SuppressWarnings("nullness")
  @Override public <T extends /*@Nullable*/ Object> /*@Nullable*/ T[] toArray(T[] array) {
    if (array.length == 0) {
      array = ObjectArrays.newArray(array, 1);
    } else if (array.length > 1) {
      array[1] = null;
    }
    // Writes will produce ArrayStoreException when the toArray() doc requires.
    /*@Nullable*/ Object[] objectArray = array;
    objectArray[0] = element;
    return array;
  }
}
