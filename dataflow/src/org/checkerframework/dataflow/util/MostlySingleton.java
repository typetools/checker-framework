package org.checkerframework.dataflow.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * A set that is more efficient than HashSet for 0 and 1 elements.
 */
final public class MostlySingleton<T> implements Set<T> {
  private enum State {
    EMPTY, SINGLETON, MORE
  }
  private State state = State.EMPTY;
  private T value;
  private HashSet<T> set;
  
  @Override
  public int size() {
    switch (state) {
    case EMPTY:
      return 0;
    case SINGLETON:
      return 1;
    default: // MORE
      return set.size();
    }
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean contains(Object o) {
    switch (state) {
    case EMPTY:
      return false;
    case SINGLETON:
      return Objects.equals(o, value);
    default: // MORE
      return set.contains(o);
    }
  }

  @Override
  @SuppressWarnings("fallthrough")	
  public boolean add(T e) {
    switch (state) {
    case EMPTY:
      state = State.SINGLETON;
      value = e;
      return true;
    case SINGLETON:
      state = State.MORE;
      set = new HashSet<T>();
      set.add(value);
      value = null;
      // fallthrough
    default: // MORE
      return set.add(e);
    }
  }

  @Override
  public Iterator<T> iterator() {
    switch (state) {
    case EMPTY:
      return Collections.emptyIterator();
    case SINGLETON:
      return new Iterator<T>() {
        private boolean hasNext = true;

        @Override
        public boolean hasNext() {
          return hasNext;
        }
        
        @Override
        public T next() {
          if (hasNext) {
            hasNext = false;
            return value;
          }
          throw new NoSuchElementException();
        }
        
        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    default: // MORE
      return set.iterator();
    }
  }

  @Override
  public Object[] toArray() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    throw new UnsupportedOperationException();  
  }
  
  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();  
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }
}
