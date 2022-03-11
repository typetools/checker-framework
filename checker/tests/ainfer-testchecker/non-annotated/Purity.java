// Copied from the all-systems tests, because of the expected error
// on line 23. During the first round of WPI, errors are warnings: so the test fails.
// The class has been renamed so that an -AskipDefs=TestPure command-line argument
// can suppress the original errors.

import org.checkerframework.dataflow.qual.*;

interface PureFunc {
  @Pure
  String doNothing();
}

class TestPure1 {

  static String myMethod() {
    return "";
  }

  @Pure
  static String myPureMethod() {
    return "";
  }

  void context() {
    PureFunc f1 = TestPure1::myPureMethod;
    // :: warning: (purity.methodref)
    PureFunc f2 = TestPure1::myMethod;
  }
}
