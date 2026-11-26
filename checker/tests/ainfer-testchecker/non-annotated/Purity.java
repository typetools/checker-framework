// Copied from framework/tests/flow/PurityMethodRef.java.  Under -Ainfer, a method reference is
// used to infer the purity of the functional interface method, and is not checked; so the
// inference run reports no purity.methodref error.  The class has been renamed so that an
// -AskipDefs=TestPure command-line argument can suppress the error that a run without -Ainfer
// reports.

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
    PureFunc f2 = TestPure1::myMethod;
  }
}
