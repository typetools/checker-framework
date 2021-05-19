// Test case for Issue 1046:
// https://github.com/typetools/checker-framework/issues/1046
// Additional test case: checker/tests/nullness/java8/Issue1046.java

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue1046Java7 {
  interface MyInterface {}

  class MyClass implements MyInterface {}

  class Function<T> {}

  abstract static class NotSubtype2 {
    static void transform(Function<? super MyClass> q) {}

    static void transform2(Function<? super @Nullable MyClass> q) {}

    void test1(Function<? super MyInterface> p, Function<? super @Nullable MyInterface> p2) {
      transform(p);
      // :: error: (argument)
      transform2(p);
      transform(p2);
      transform2(p2);
    }

    @Nullable Function<Object> NULL = null;

    <T> void test2(@Nullable Function<? super @NonNull T> queue) {
      Function<? super @NonNull T> x = (queue == null) ? NULL : queue;
    }
  }
}
