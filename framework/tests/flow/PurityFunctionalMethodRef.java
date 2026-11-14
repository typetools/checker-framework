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

  void bodyDoesNotRelyOnParameter() {
    // None of these bodies relies on the purity of f, so no call can make one run impure code.
    UnannotatedApplier a = PurityFunctionalMethodRef::ignoreStatic;
    UnannotatedApplier b = PurityFunctionalMethodRef::storeStatic;
    UnannotatedApplier c = this::ignoreFinal;
    // An override of ignoreOverridable might call f.
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier d = this::ignoreOverridable;
    // passStatic relies on the purity of f to call applyStatic.
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier e = PurityFunctionalMethodRef::passStatic;
  }

  void contravariantCallbackParameter() {
    // A call through SubApplier passes a PureFunction, whose apply is what applyStatic's call to
    // Function.apply runs.
    SubApplier a = PurityFunctionalMethodRef::applyStatic;
  }
}
