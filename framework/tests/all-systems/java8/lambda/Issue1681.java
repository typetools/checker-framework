// Test case for Issue 1681:
// https://github.com/typetools/checker-framework/issues/1681

public class Issue1681 {
  @FunctionalInterface
  interface StrReturn {
    String op();
  }

  StrReturn[] v1 = new StrReturn[] {() -> "hello"};
  StrReturn[] v2 = new StrReturn[] {() -> "hello", () -> "world"};
  StrReturn[] v3 = {() -> "test"};

  void foo(StrReturn[] p) {}

  void bar() {
    foo(new StrReturn[] {() -> "test", () -> "boo"});
  }
}
