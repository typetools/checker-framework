// Test case for Issue 1864:
// https://github.com/typetools/checker-framework/issues/1864

public class Issue1864b {
  interface Supplier {
    Object get();
  }

  Supplier foo() {
    Object foo = new Object();
    return () -> foo;
  }
}
