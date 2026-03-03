// Test case for Issue 6433:
// https://github.com/typetools/checker-framework/issues/6433
class Issue6433 {
  void foo() {}

  void bar() {
    foo();
    while (true) {}
  }

  void baz() {
    foo();
    while (true) {
      break;
    }
  }
}
