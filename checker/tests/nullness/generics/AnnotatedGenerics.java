import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;

public class AnnotatedGenerics {
  public static void testNullableTypeVariable() {
    class Test<T extends @Nullable Object> {
      // :: error: (initialization.field.uninitialized)
      T f;

      @Nullable T get() {
        return f;
      }
    }
    Test<Iterable<String>> l = new Test<>();
    // :: error: (iterating.over.nullable)
    for (String s : l.get()) {}
  }

  public static void testNonNullTypeVariable() {
    class Test<T extends @Nullable Object> {
      @NonNull T get() {
        throw new RuntimeException();
      }
    }
    Test<@Nullable Iterable<String>> l = new Test<>();
    for (String s : l.get()) {}
    Test<Iterable<String>> n = new Test<>();
    for (String s : n.get()) {}
  }

  static class MyClass<T> implements MyIterator<@Nullable T> {
    public boolean hasNext() {
      return true;
    }

    public @Nullable T next() {
      return null;
    }

    public void remove() {}

    static void test() {
      MyClass<String> c = new MyClass<>();
      String c1 = c.next();
      @Nullable String c2 = c.next();
      // :: error: (assignment)
      @NonNull String c3 = c.next();
    }
  }

  public static final class MyComprator<T extends MyComparable<T>> {
    public void compare(T a1, T a2) {
      a1.compareTo(a2);
    }

    public void compare2(@NonNull T a1, @NonNull T a2) {
      a1.compareTo(a2);
    }

    public void compare3(T a1, @Nullable T a2) {
      // :: error: (argument)
      a1.compareTo(a2);
    }
  }

  class MyComparable<T> {
    @Pure
    public int compareTo(@NonNull T a1) {
      return 0;
    }
  }

  <T> T test(java.util.List<? super Iterable<?>> l) {
    test(new java.util.ArrayList<Object>());
    throw new Error();
  }

  public interface MyIterator<E extends @Nullable Object> {
    boolean hasNext();

    E next();

    void remove();
  }
}
