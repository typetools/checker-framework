// Test that a method reference is checked against the purity annotations on the functional
// interface method that it implements.  This is the method-reference analog of
// PurityLambdaSam.java; the check is performed by
// OverrideChecker#checkMethodReferencePurity, which treats the referenced method as an override
// of the functional method.
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

  /**
   * Inherits two functional methods, {@code SefFunc.doNothing} and {@code PlainFunc.doNothing},
   * neither of which overrides the other.
   */
  @FunctionalInterface
  interface SefFirstFunc extends SefFunc, PlainFunc {}

  /** Like {@link SefFirstFunc}, but with its superinterfaces in the other order. */
  @FunctionalInterface
  interface PlainFirstFunc extends PlainFunc, SefFunc {}

  void inheritedFunctionalMethods() {
    // A call through SefFunc.doNothing can run the reference.
    // :: error: [purity.methodref]
    SefFirstFunc f1 = PurityMethodRef::myMethod;
    // :: error: [purity.methodref]
    PlainFirstFunc f2 = PurityMethodRef::myMethod;
    SefFirstFunc f3 = PurityMethodRef::mySideEffectFreeMethod;
    PlainFirstFunc f4 = PurityMethodRef::mySideEffectFreeMethod;
  }

  void intersectionCast() {
    // A call through SefFunc.doNothing can run the reference.
    // :: error: [purity.methodref]
    Object f1 = (PlainFunc & SefFunc) PurityMethodRef::myMethod;
    // :: error: [purity.methodref]
    Object f2 = (SefFunc & PlainFunc) PurityMethodRef::myMethod;
    Object f3 = (PlainFunc & SefFunc) PurityMethodRef::mySideEffectFreeMethod;
    Object f4 = (PlainFunc & java.io.Serializable) PurityMethodRef::myMethod;
    // The first bound is not a functional interface.
    // :: error: [purity.methodref]
    Object f5 = (java.io.Serializable & SefFunc) PurityMethodRef::myMethod;
  }

  /** Like {@link SefFunc}, but with a less specific return type. */
  @FunctionalInterface
  interface SefObjectFunc {
    @SideEffectFree
    Object doNothing();
  }

  static <T extends SefObjectFunc & PlainFunc> void takesSefObjectAndPlain(T f) {}

  void intersectionTargets() {
    // The functional method is PlainFunc.doNothing, whose return type is more specific than that of
    // SefObjectFunc.doNothing.  A call through SefObjectFunc.doNothing can run the reference.
    // :: error: [purity.methodref]
    Object f1 = (SefObjectFunc & PlainFunc) PurityMethodRef::myMethod;
    Object f2 = (SefObjectFunc & PlainFunc) PurityMethodRef::mySideEffectFreeMethod;
    // The target is an intersection type that is inferred rather than written in a cast.
    // :: error: [purity.methodref]
    takesSefObjectAndPlain(PurityMethodRef::myMethod);
    takesSefObjectAndPlain(PurityMethodRef::mySideEffectFreeMethod);
  }

  @FunctionalInterface
  interface GenSefFunc<T> {
    @SideEffectFree
    T doNothing();
  }

  @FunctionalInterface
  interface GenPlainFunc<T> {
    T doNothing();
  }

  @FunctionalInterface
  interface GenPlainFirstFunc<T> extends GenPlainFunc<T>, GenSefFunc<T> {}

  void genericSuperinterfaces() {
    // :: error: [purity.methodref]
    GenPlainFirstFunc<String> f1 = PurityMethodRef::myMethod;
    GenPlainFirstFunc<String> f2 = PurityMethodRef::mySideEffectFreeMethod;
    // A bound with a wildcard type argument, which javac replaces by its non-wildcard
    // parameterization.  javac rejects such a bound when another bound is a functional interface.
    // :: error: [purity.methodref]
    Object f3 = (java.io.Serializable & GenSefFunc<? extends String>) PurityMethodRef::myMethod;
    Object f4 =
        (java.io.Serializable & GenSefFunc<? extends String>)
            PurityMethodRef::mySideEffectFreeMethod;
    Object f5 = (java.io.Serializable & GenPlainFunc<? extends String>) PurityMethodRef::myMethod;
  }

  /** Inherits {@code SefFunc.doNothing} and {@code PureFunc.doNothing}, which differ in purity. */
  @FunctionalInterface
  interface SefAndPureFunc extends SefFunc, PureFunc {}

  /** Inherits {@code SefFunc.doNothing} and {@code DetFunc.doNothing}, which differ in purity. */
  @FunctionalInterface
  interface SefAndDetFunc extends SefFunc, DetFunc {}

  /**
   * The errors together name every purity kind that the referenced method lacks, and no error names
   * only kinds that another error names.
   */
  void differentPurities() {
    // One error, for PureFunc.doNothing, which requires everything that SefFunc.doNothing does.
    // :: error: [purity.methodref]
    SefAndPureFunc f1 = PurityMethodRef::myMethod;
    // :: error: [purity.methodref]
    SefAndPureFunc f2 = PurityMethodRef::mySideEffectFreeMethod;
    SefAndPureFunc f3 = PurityMethodRef::myPureMethod;
    // Two errors, one for each declaration.
    // :: error: [purity.methodref]
    // :: error: [purity.methodref]
    SefAndDetFunc f4 = PurityMethodRef::myMethod;
    // :: error: [purity.methodref]
    SefAndDetFunc f5 = PurityMethodRef::myDeterministicMethod;
  }

  /**
   * A functional interface whose method is named {@code clone}. {@code Object.clone} is protected,
   * so it does not prevent {@code Copier} from being a functional interface.
   */
  @FunctionalInterface
  interface Copier {
    Object clone();
  }

  /** {@code Object.clone} is {@code @SideEffectFree}, but it does not constrain the reference. */
  void cloneMethod() {
    Copier c = PurityMethodRef::myMethod;
  }

  @FunctionalInterface
  interface PureRunnable {
    @Pure
    // :: warning: [purity.deterministic.void.method]
    void run();
  }

  @SideEffectFree
  static void mySideEffectFreeVoidMethod() {}

  @Deterministic
  // :: warning: [purity.deterministic.void.method]
  static void myDeterministicVoidMethod() {}

  /** A side-effect-free method that returns void is deterministic. */
  void voidMethods() {
    PureRunnable r1 = PurityMethodRef::mySideEffectFreeVoidMethod;
    // :: error: [purity.methodref]
    PureRunnable r2 = PurityMethodRef::myDeterministicVoidMethod;
  }
}
