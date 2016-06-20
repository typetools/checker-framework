// Test case for Issue 811
// https://github.com/typetools/checker-framework/issues/811
// @skip-test
import org.checkerframework.checker.nullness.qual.NonNull;

class A {
  static class T {
    void xyz() {}
  }

  interface U {
    void method();
  }

  private final @NonNull T tField;
  private U uField;
  public A(@NonNull T t) {
    tField = t;
    uField = new U() {
      @Override
      public void method() {
        tField.xyz();
      }
    };
  }
}
