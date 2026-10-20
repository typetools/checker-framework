// Without -AcheckPurityAnnotations, no purity annotation is checked, so no purity diagnostic is
// issued.  This file holds the violations that framework/tests/flow/PurityMethodRef.java and
// PurityLambdaSam.java assert, and expects no diagnostic for any of them.

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityMethodRefNotChecked {

  int count = 0;

  @FunctionalInterface
  interface PureFunc {
    @Pure
    String doNothing();
  }

  @FunctionalInterface
  interface SefFunc {
    @SideEffectFree
    String doNothing();
  }

  static String myMethod() {
    return "";
  }

  /** A method reference to a method that lacks the functional method's purity. */
  void methodReference() {
    PureFunc f = PurityMethodRefNotChecked::myMethod;
    SefFunc g = PurityMethodRefNotChecked::myMethod;
  }

  /** A lambda whose body lacks the functional method's purity. */
  void lambda() {
    SefFunc f = () -> "" + count++;
  }

  /** A method whose body lacks its own purity. */
  @SideEffectFree
  int assignsField() {
    return count++;
  }
}
