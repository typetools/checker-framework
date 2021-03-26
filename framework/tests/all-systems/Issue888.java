// Test case for Issue #888
// https://github.com/typetools/checker-framework/issues/888

public class Issue888 {
  <T> T foo(T t) {
    return t;
  }

  void bar(int i) {
    foo(i).toString();
  }
}
