/**
 * Reported modifiers depend on the command-line invocation; see
 * org.checkerframework.checker/tests/src/tests/ReportModifiers.java
 */
public class TestModifiers {
  void test() {
    class Inner {
      // :: error: (Modifier.native)
      native void bad();
    }
  }
}
