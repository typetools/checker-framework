// Test that a method reference is checked against the purity annotations on the functional
// interface method that it implements.  This is the method-reference analog of
// PurityLambdaSam.java; the check is performed by BaseTypeVisitor.OverrideChecker#checkPurity,
// which treats the referenced method as an override of the functional method.
//
// This test lives here, rather than in all-systems, because the check requires
// -AcheckPurityAnnotations, which the all-systems suite is not run with.  That purity annotations
// go unchecked without that option is tested by framework/tests/purity-not-checked.

import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityMethodRef {

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

  @FunctionalInterface
  interface DetFunc {
    @Deterministic
    String doNothing();
  }

  /** No purity annotation, so any method may be referenced. */
  @FunctionalInterface
  interface PlainFunc {
    String doNothing();
  }

  static String myMethod() {
    return "";
  }

  @SideEffectFree
  static String mySideEffectFreeMethod() {
    return "";
  }

  @Deterministic
  static String myDeterministicMethod() {
    return "";
  }

  @Pure
  static String myPureMethod() {
    return "";
  }

  void context() {
    PureFunc f1 = PurityMethodRef::myPureMethod;
    // :: error: [purity.methodref]
    PureFunc f2 = PurityMethodRef::myMethod;
    // A method with one of the two kinds does not satisfy @Pure, which requires both.
    // :: error: [purity.methodref]
    PureFunc f3 = PurityMethodRef::mySideEffectFreeMethod;
    // :: error: [purity.methodref]
    PureFunc f4 = PurityMethodRef::myDeterministicMethod;
  }

  /** Each kind constrains only itself. */
  void eachKind() {
    SefFunc sef = PurityMethodRef::mySideEffectFreeMethod;
    // :: error: [purity.methodref]
    SefFunc notSef = PurityMethodRef::myDeterministicMethod;
    DetFunc det = PurityMethodRef::myDeterministicMethod;
    // :: error: [purity.methodref]
    DetFunc notDet = PurityMethodRef::mySideEffectFreeMethod;
  }

  /** An unannotated functional method promises nothing, so it constrains nothing. */
  void unconstrained() {
    PlainFunc f = PurityMethodRef::myMethod;
  }
}
