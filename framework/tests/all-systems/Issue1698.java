// Test case for Issue 1698:
// https://github.com/typetools/checker-framework/issues/1698

public class Issue1698 {
  static class B {
    static C f() {
      return new B().new C();
    }

    class C {}
  }
}
