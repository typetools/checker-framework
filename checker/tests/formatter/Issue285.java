// Test case for Issue 285:
// https://github.com/typetools/checker-framework/issues/285
public class Issue285 {
  void f() {
    for (String s : new String[] {"s"}) {}
  }
}
