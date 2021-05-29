// Test case for issue #764:
// https://github.com/typetools/checker-framework/issues/764

import org.checkerframework.checker.nullness.qual.*;

public class Issue764 {
  public static @Nullable Object field = null;

  static class MyClass {
    @RequiresNonNull("field")
    public static void method() {}

    public void otherMethod() {
      field = new Object();
      method();
    }

    public void otherMethod2() {
      // :: error: (contracts.precondition)
      method();
    }
  }
}
