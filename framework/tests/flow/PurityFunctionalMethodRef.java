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
    // A call through UnannotatedApplier checks nothing, so it may run applyStatic with a function
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
}
