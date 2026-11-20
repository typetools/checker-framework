// A method reference is checked like an override: a call through the functional method must
// guarantee, of the arguments to the referenced method's functional-interface parameters, the
// purity that the referenced method's body assumes of them.

import java.util.function.Function;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityFunctionalMethodRef {

  @SideEffectFree
  static int applyStatic(Function<String, Integer> f, String s) {
    return f.apply(s);
  }

  @SideEffectFree
  int applyInstance(Function<String, Integer> f, String s) {
    return f.apply(s);
  }

  @FunctionalInterface
  interface Applier {
    @SideEffectFree
    int apply(Function<String, Integer> f, String s);
  }

  @FunctionalInterface
  interface UnboundApplier {
    @SideEffectFree
    int apply(PurityFunctionalMethodRef receiver, Function<String, Integer> f, String s);
  }

  @FunctionalInterface
  interface GenericApplier<T> {
    @SideEffectFree
    int apply(T f, String s);
  }

  @FunctionalInterface
  interface UnannotatedApplier {
    int apply(Function<String, Integer> f, String s);
  }

  void references() {
    Applier a = PurityFunctionalMethodRef::applyStatic;
    Applier b = this::applyInstance;
    UnboundApplier c = PurityFunctionalMethodRef::applyInstance;
    // :: error: [purity.methodref.functional.parameter]
    GenericApplier<Function<String, Integer>> d = PurityFunctionalMethodRef::applyStatic;
    // A call through UnannotatedApplier checks nothing, so it can run applyStatic with a function
    // that has side effects.
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier e = PurityFunctionalMethodRef::applyStatic;
  }

  /** The body relies on the purity of calls to u.apply, but those calls check no argument. */
  @SideEffectFree
  static int runsUnannotated(UnannotatedApplier u) {
    return u.apply(s -> 0, "");
  }

  /** The body relies on the purity of calls to a.apply, and those calls check their argument. */
  @SideEffectFree
  static int runsAnnotated(Applier a) {
    return a.apply(s -> 0, "");
  }

  void passesReferences() {
    // :: error: [purity.methodref.functional.parameter]
    runsUnannotated(PurityFunctionalMethodRef::applyStatic);
    runsAnnotated(PurityFunctionalMethodRef::applyStatic);
  }

  /** A subinterface that redeclares {@code Function.apply}, with a stronger guarantee. */
  @FunctionalInterface
  interface PureFunction extends Function<String, Integer> {
    @Override
    @SideEffectFree
    Integer apply(String s);
  }

  @FunctionalInterface
  interface SubApplier {
    @SideEffectFree
    int apply(PureFunction f, String s);
  }

  /** Redeclares {@code UnannotatedApplier.apply}, which a call can still go through. */
  @FunctionalInterface
  interface StrongApplier extends UnannotatedApplier {
    @Override
    @SideEffectFree
    int apply(Function<String, Integer> f, String s);
  }

  void overriddenFunctionalMethod() {
    // A call through UnannotatedApplier.apply can run the reference.
    // :: error: [purity.methodref.functional.parameter]
    StrongApplier a = PurityFunctionalMethodRef::applyStatic;
  }

  @SideEffectFree
  static int ignoreStatic(Function<String, Integer> f, String s) {
    return 0;
  }

  @SideEffectFree
  static int storeStatic(Function<String, Integer> f, String s) {
    Function<String, Integer> g = f;
    return f == null ? 0 : f.hashCode();
  }

  @SideEffectFree
  final int ignoreFinal(Function<String, Integer> f, String s) {
    return 0;
  }

  @SideEffectFree
  int ignoreOverridable(Function<String, Integer> f, String s) {
    return 0;
  }

  @SideEffectFree
  static int passStatic(Function<String, Integer> f, String s) {
    return applyStatic(f, s);
  }

  @SideEffectFree
  static int overwriteStatic(Function<String, Integer> f, String s) {
    f = null;
    return 0;
  }

  @SideEffectFree
  static int overwriteArrayStatic(Function<String, Integer> f, String s) {
    int[] a = new int[1];
    // :: error: [purity.assign.array]
    a[f.apply(s)] = 0;
    return 0;
  }

  void bodyDoesNotRelyOnParameter() {
    // None of these bodies relies on the purity of f, so no call can make one run impure code.
    UnannotatedApplier a = PurityFunctionalMethodRef::ignoreStatic;
    UnannotatedApplier b = PurityFunctionalMethodRef::storeStatic;
    UnannotatedApplier c = this::ignoreFinal;
    UnannotatedApplier f = PurityFunctionalMethodRef::overwriteStatic;
    // The index of the array access calls f.
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier g = PurityFunctionalMethodRef::overwriteArrayStatic;
    // An override of ignoreOverridable might call f.
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier d = this::ignoreOverridable;
    // passStatic relies on the purity of f to call applyStatic.
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier e = PurityFunctionalMethodRef::passStatic;
  }

  /** A subinterface that redeclares {@code Function.apply}, with no purity annotation. */
  @FunctionalInterface
  interface PlainFunction extends Function<String, Integer> {
    @Override
    Integer apply(String s);
  }

  @FunctionalInterface
  interface PlainSubApplier {
    @SideEffectFree
    int apply(PlainFunction f, String s);
  }

  void contravariantCallbackParameter() {
    // A call through SubApplier passes a PureFunction, whose apply is what applyStatic's call to
    // Function.apply runs.
    SubApplier a = PurityFunctionalMethodRef::applyStatic;
    // PlainFunction.apply promises nothing, but a call through PlainSubApplier.apply checks that
    // its argument's implementation of PlainFunction.apply, which is also its implementation of
    // Function.apply, is side-effect-free.
    PlainSubApplier b = PurityFunctionalMethodRef::applyStatic;
  }

  /** A class, not a functional interface, that implements {@code Function.apply} purely. */
  static final class PureImpl implements Function<String, Integer> {
    @Override
    @SideEffectFree
    public Integer apply(String s) {
      return 0;
    }
  }

  /** A class, not a functional interface, that implements {@code Function.apply} impurely. */
  static final class ImpureImpl implements Function<String, Integer> {
    @Override
    public Integer apply(String s) {
      return 0;
    }
  }

  @FunctionalInterface
  interface PureImplApplier {
    int apply(PureImpl f, String s);
  }

  @FunctionalInterface
  interface ImpureImplApplier {
    int apply(ImpureImpl f, String s);
  }

  void classCallbackParameter() {
    // Every argument is a PureImpl, whose apply is side-effect-free.
    PureImplApplier a = PurityFunctionalMethodRef::applyStatic;
    // :: error: [purity.methodref.functional.parameter]
    ImpureImplApplier b = PurityFunctionalMethodRef::applyStatic;
  }

  /**
   * Implements {@code Function.apply} with a side-effect-free default method, and declares an
   * unrelated functional method.
   */
  @FunctionalInterface
  interface PureDefaultFunction extends Function<String, Integer> {
    @Override
    @SideEffectFree
    default Integer apply(String s) {
      return 0;
    }

    int other();
  }

  /** Implements {@code Function.apply} with a default method that has no purity annotation. */
  @FunctionalInterface
  interface ImpureDefaultFunction extends Function<String, Integer> {
    @Override
    default Integer apply(String s) {
      return 0;
    }

    int other();
  }

  @FunctionalInterface
  interface PureDefaultApplier {
    @SideEffectFree
    int apply(PureDefaultFunction f, String s);
  }

  @FunctionalInterface
  interface ImpureDefaultApplier {
    @SideEffectFree
    int apply(ImpureDefaultFunction f, String s);
  }

  void defaultImplementationOfCallback() {
    // applyStatic calls Function.apply, which a PureDefaultFunction implements with a
    // side-effect-free method; a call through PureDefaultApplier checks only `other`.
    PureDefaultApplier a = PurityFunctionalMethodRef::applyStatic;
    // :: error: [purity.methodref.functional.parameter]
    ImpureDefaultApplier b = PurityFunctionalMethodRef::applyStatic;
  }

  /** A class, not a functional interface, whose implementation of apply is side-effect-free. */
  static final class PureFixedFunction implements Function<String, Integer> {
    @Override
    @SideEffectFree
    public Integer apply(String s) {
      return 0;
    }
  }

  /** A class whose implementation of apply has no purity annotation. */
  static final class ImpureFixedFunction implements Function<String, Integer> {
    @Override
    public Integer apply(String s) {
      return 0;
    }
  }

  @FunctionalInterface
  interface PureFixedApplier {
    int apply(PureFixedFunction f, String s);
  }

  @FunctionalInterface
  interface ImpureFixedApplier {
    int apply(ImpureFixedFunction f, String s);
  }

  void classTypedCallback() {
    // A call through PureFixedApplier checks nothing, but every PureFixedFunction implements
    // Function.apply with a side-effect-free method.
    PureFixedApplier a = PurityFunctionalMethodRef::applyStatic;
    // :: error: [purity.methodref.functional.parameter]
    ImpureFixedApplier b = PurityFunctionalMethodRef::applyStatic;
  }
}
