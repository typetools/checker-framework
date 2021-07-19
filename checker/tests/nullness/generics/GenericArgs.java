import java.io.*;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;

@org.checkerframework.framework.qual.DefaultQualifier(Nullable.class)
public class GenericArgs {

  public @NonNull Set<@NonNull String> strings = new HashSet<>();

  void test() {
    @NonNull HashSet<@NonNull String> s = new HashSet<>();

    strings.addAll(s);
    strings.add("foo");
  }

  static class X<@NonNull T extends @NonNull Object> {
    T value() {
      // :: error: (return)
      return null;
    }
  }

  public static void test2() {
    // :: error: (type.argument)
    Object o = new X<Object>().value();
  }

  static <@NonNull Z extends @NonNull Object> void test3(Z z) {}

  void test4() {
    // :: error: (type.argument)
    GenericArgs.<@Nullable Object>test3(null);
    // :: error: (argument)
    GenericArgs.<@NonNull Object>test3(null);
  }

  static class GenericConstructor {
    <@NonNull T extends @NonNull Object> GenericConstructor(T t) {}
  }

  void test5() {
    // :: error: (argument)
    new <@NonNull String>GenericConstructor(null);
  }

  void testRecursiveDeclarations() {
    class MyComparator<@NonNull T extends @NonNull Comparable<T>>
        implements Comparator<T @NonNull []> {
      @Pure
      public int compare(T[] a, T[] b) {
        return 0;
      }
    }
    Comparator<@NonNull String @NonNull []> temp = new MyComparator<@NonNull String>();
  }
}
