// Test case for Issue #671
// https://github.com/typetools/checker-framework/issues/671
public class Issue671 {

  void foo() {
    byte var = 0;
    boolean f = (var == (method() ? 2 : 0));
  }

  boolean method() {
    return false;
  }
}
