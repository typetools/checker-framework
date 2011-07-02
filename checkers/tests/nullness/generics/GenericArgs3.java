import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import checkers.nullness.quals.Nullable;

class Other {
    public static final class StaticIterator<T> implements Iterator<T> {
        Enumeration<T> e;
        public StaticIterator(Enumeration<T> e) { this.e = e; }
        public boolean hasNext() { return e.hasMoreElements(); }
        public T next() { return e.nextElement(); }
        public void remove() { throw new UnsupportedOperationException(); }
      }

    public final class FinalIterator<T> implements Iterator<T> {
        Enumeration<T> e;
        public FinalIterator(Enumeration<T> e) { this.e = e; }
        public boolean hasNext() { return e.hasMoreElements(); }
        public T next() { return e.nextElement(); }
        public void remove() { throw new UnsupportedOperationException(); }
      }
}

class Entry<K,V> implements Map.Entry<K,V> {
    public V setValue(V newValue) { throw new RuntimeException(); }
    public K getKey() { throw new RuntimeException(); }
    public V getValue() { throw new RuntimeException(); }
}

interface Function<F, T extends @Nullable Object> {
  T apply(@Nullable F from);
  boolean equals(@Nullable Object obj);
}

enum IdentityFunction implements Function<Object, @Nullable Object> {
  INSTANCE;
  public @Nullable Object apply(@Nullable Object o) {
    return o;
  }
}

abstract class FilteredCollection<E> implements Collection<E> {
  public boolean addAll(Collection<? extends E> collection) {
    for (E element : collection) {
    }
    return true;
  }
}
