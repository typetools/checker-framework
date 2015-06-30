/**
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
import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Multisets.setCountImpl;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.*;

/**
 * This class provides a skeletal implementation of the {@link Multiset}
 * interface. A new multiset implementation can be created easily by extending
 * this class and implementing the {@link Multiset#entrySet()} method, plus
 * optionally overriding {@link #add(Object, int)} and
 * {@link #remove(Object, int)} to enable modifications to the multiset.
 *
 * <p>The {@link #contains}, {@link #containsAll}, {@link #count}, and
 * {@link #size} implementations all iterate across the set returned by
 * {@link Multiset#entrySet()}, as do many methods acting on the set returned by
 * {@link #elementSet()}. Override those methods for better performance.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
@SuppressWarnings("nullness:generic.argument")
abstract class AbstractMultiset<E extends /*@Nullable*/ Object> extends AbstractCollection<E>
    implements Multiset<E> {
    @SideEffectFree public abstract Set<Entry<E>> entrySet();

  // Query Operations

  @Pure @Override public int size() {
    long sum = 0L;
    for (Entry<E> entry : entrySet()) {
      sum += entry.getCount();
    }
    return (int) Math.min(sum, Integer.MAX_VALUE);
  }

  @Pure @Override public boolean isEmpty() {
    return entrySet().isEmpty();  }

  @SuppressWarnings("nullness")
      //Suppressed due to annotation on Set
  @Pure @Override public boolean contains(@Nullable Object element) {
    return elementSet().contains(element);
  }

  @Override public Iterator<E> iterator() {
    return new MultisetIterator();
  }

  private class MultisetIterator implements Iterator<E> {
    private final Iterator<Entry<E>> entryIterator;
    private Entry<E> currentEntry;
    /** Count of subsequent elements equal to current element */
    private int laterCount;
    /** Count of all elements equal to current element */
    private int totalCount;
    private boolean canRemove;

    MultisetIterator() {
      this.entryIterator = entrySet().iterator();
    }

    public boolean hasNext() {
      return laterCount > 0 || entryIterator.hasNext();
    }

    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      if (laterCount == 0) {
        currentEntry = entryIterator.next();
        totalCount = laterCount = currentEntry.getCount();
      }
      laterCount--;
      canRemove = true;
      return currentEntry.getElement();
    }

    public void remove() {
      checkState(canRemove,
          "no calls to next() since the last call to remove()");
      if (totalCount == 1) {
        entryIterator.remove();
      } else {
        AbstractMultiset.this.remove(currentEntry.getElement());
      }
      totalCount--;
      canRemove = false;
    }
  }

  public int count(/*@Nullable*/ Object element) {
    for (Entry<E> entry : entrySet()) {
      if (Objects.equal(entry.getElement(), element)) {
        return entry.getCount();
      }
    }
    return 0;
  }

  // Modification Operations

  @Override public boolean add(E element) {
    add(element, 1);
    return true;
  }

  public int add(E element, int occurrences) {
    throw new UnsupportedOperationException();
  }

  @Override public boolean remove(/*@Nullable*/ Object element) {
    return remove(element, 1) > 0;
  }

  public int remove(/*@Nullable*/ Object element, int occurrences) {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("nullness")
  public int setCount(E element, int count) {
    return setCountImpl(this, element, count);
  }

  @SuppressWarnings("nullness")
  public boolean setCount(E element, int oldCount, int newCount) {
    return setCountImpl(this, element, oldCount, newCount);
  }

  // Bulk Operations

  @SuppressWarnings("nullness")
  // Suppressed Warnings to override containsAll in Set<E>
  @Pure @Override public boolean containsAll(Collection<? extends /*@Nullable*/ Object> elements) {
    return elementSet().containsAll(elements);
  }

  @Override public boolean addAll(Collection<? extends E> elementsToAdd) {
    if (elementsToAdd.isEmpty()) {
      return false;
    }
    if (elementsToAdd instanceof Multiset) {
      @SuppressWarnings("unchecked")
      Multiset<? extends E> that = (Multiset<? extends E>) elementsToAdd;
      for (Entry<? extends E> entry : that.entrySet()) {
        add(entry.getElement(), entry.getCount());
      }

    } else {
      super.addAll(elementsToAdd);
    }
    return true;
  }

  @Override public boolean removeAll(Collection<? extends /*@Nullable*/ Object> elementsToRemove) {
    Collection<?> collection = (elementsToRemove instanceof Multiset)
        ? ((Multiset<?>) elementsToRemove).elementSet() : elementsToRemove;

    return elementSet().removeAll(collection);
    // TODO: implement retainAll similarly?
  }

  @SuppressWarnings("nullness")
  @Override public boolean retainAll(Collection<? extends /*@Nullable*/ Object> elementsToRetain) {
    checkNotNull(elementsToRetain);
    Iterator<Entry<E>> entries = entrySet().iterator();
    boolean modified = false;
    while (entries.hasNext()) {
      Entry<E> entry = entries.next();
      if (!elementsToRetain.contains(entry.getElement())) {
        entries.remove();
        modified = true;
      }
    }
    return modified;
  }

  @Override public void clear() {
    entrySet().clear();
  }

  // Views

  private transient volatile Set<E> elementSet;

  @SideEffectFree public Set<E> elementSet() {
    Set<E> result = elementSet;
    if (result == null) {
      elementSet = result = createElementSet();
    }
    return result;
  }

  /**
   * Creates a new instance of this multiset's element set, which will be
   * returned by {@link #elementSet()}.
   */
  Set<E> createElementSet() {
    return new ElementSet();
  }

  private class ElementSet extends AbstractSet<E> {
    @Override public Iterator<E> iterator() {
      final Iterator<Entry<E>> entryIterator = entrySet().iterator();
      return new Iterator<E>() {
        public boolean hasNext() {
          return entryIterator.hasNext();
        }
        public E next() {
          return entryIterator.next().getElement();
        }
        public void remove() {
          entryIterator.remove();
        }
      };
    }
    @Pure @Override public int size() {
      return entrySet().size();
    }
  }

  // Object methods

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns {@code true} if {@code other} is a multiset
   * of the same size and if, for each element, the two multisets have the same
   * count.
   */
  @Pure @Override public boolean equals(@Nullable Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof Multiset) {
      Multiset<?> that = (Multiset<?>) object;
      /*
       * We can't simply check whether the entry sets are equal, since that
       * approach fails when a TreeMultiset has a comparator that returns 0
       * when passed unequal elements.
       */

      if (this.size() != that.size()) {
        return false;
      }
      for (Entry<?> entry : that.entrySet()) {
        if (count(entry.getElement()) != entry.getCount()) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns the hash code of {@link
   * Multiset#entrySet()}.
   */
  @Pure @Override public int hashCode() {
    return entrySet().hashCode();
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns the result of invoking {@code toString} on
   * {@link Multiset#entrySet()}.
   */
  @Pure @Override public String toString() {
    return entrySet().toString();
  }
}
