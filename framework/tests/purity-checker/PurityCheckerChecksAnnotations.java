// The Purity Checker checks purity annotations even without -AcheckPurityAnnotations.  This file
// holds the same violations as framework/tests/purity-not-checked/PurityMethodRefNotChecked.java,
// and expects a diagnostic for each of them.

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityCheckerChecksAnnotations {

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
    // :: error: [purity.methodref]
    PureFunc f = PurityCheckerChecksAnnotations::myMethod;
    // :: error: [purity.methodref]
    SefFunc g = PurityCheckerChecksAnnotations::myMethod;
  }

  /** A lambda whose body lacks the functional method's purity. */
  void lambda() {
    // :: error: [purity.assign.field]
    SefFunc f = () -> "" + count++;
  }

  /** A method whose body lacks its own purity. */
  @SideEffectFree
  int assignsField() {
    // :: error: [purity.assign.field]
    return count++;
  }
}
