import checkers.nullness.quals.*;
import java.io.*;
import java.util.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.Nullable")
public class GenericArgs {

    public @NonNull Set<@NonNull String> strings = new HashSet<@NonNull String>();

    void test() {
        @NonNull HashSet<@NonNull String> s = new HashSet<@NonNull String>();

        strings.addAll(s);
        strings.add("foo");
    }

    static class X<T extends @NonNull Object> {
        T value() {
            return null;
        }
    }

    public static void test2() {
        Object o = new X<Object>().value();
    }

    static <Z extends @NonNull Object> void test3(Z z) {

    }

    void test4() {
        GenericArgs.<@Nullable Object>test3(null);
        GenericArgs.<@NonNull Object>test3(null);
    }

    static class GenericConstructor {
        <T extends @NonNull Object> GenericConstructor(T t) {

        }
    }

    void test5() {
        new <@NonNull String> GenericConstructor(null);
    }

    void testRecursiveDeclarations() {
        class MyComparator<T extends @NonNull Comparable<T>>
        implements Comparator<T @NonNull []> {
            public int compare(T[] a, T[] b) { return 0; }
        }
        Comparator<@NonNull String @NonNull []> temp = new MyComparator<@NonNull String>();
    }
}

// NNEL default
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
