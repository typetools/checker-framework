// Test case for Issue 905:
// https://github.com/typetools/checker-framework/issues/905

import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class Issue905 {
  final Object mBar;

  Issue905() {
    // this should be @UnderInitialization(Object.class), so this call should be forbidden.
    // :: error: (method.invocation.invalid)
    baz();
    mBar = "";
  }

  Issue905(int i) {
    mBar = "";
    baz();
  }

  void baz(@UnknownInitialization(Issue905.class) Issue905 this) {
    mBar.toString();
  }

  public static void main(String[] args) {
    new Issue905();
  }
}
