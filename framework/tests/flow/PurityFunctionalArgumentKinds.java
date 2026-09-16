// The requirement on an argument passed to a functional-interface parameter matches the purity of
// the callee:  a @Pure method's functional arguments must be @Pure, a @Deterministic method's must
// be @Deterministic, and a @SideEffectFree method's must be @SideEffectFree.
//
// Each call site is in an unannotated method, so the only diagnostic a call site can produce is the
// one under test.

import java.util.function.Function;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityFunctionalArgumentKinds {

  int count = 0;

  @Pure
  static int pureCallee(Function<String, Integer> f, String s) {
    return 0;
  }

  @Deterministic
  static int deterministicCallee(Function<String, Integer> f, String s) {
    return 0;
  }

  @SideEffectFree
  static int sideEffectFreeCallee(Function<String, Integer> f, String s) {
    return 0;
  }

  @Pure
  int pureMethod(String s) {
    return 1;
  }

  @SideEffectFree
  int sideEffectFreeOnly(String s) {
    return count;
  }

  @Deterministic
  int deterministicOnly(String s) {
    return 1;
  }

  // A @Pure or @Deterministic method may call the functional method of its own
  // functional-interface parameters.

  @Pure
  int pureCallsParameter(Function<String, Integer> f, String s) {
    return f.apply(s);
  }

  @Deterministic
  int deterministicCallsParameter(Function<String, Integer> f, String s) {
    return f.apply(s);
  }

  // A lambda argument's body is checked against the callee's purity.

  void lambdaArgumentsToPureCallee(String s) {
    pureCallee(t -> 1, s);
    // :: error: [purity.not.deterministic.not.sideeffectfree.assign.field]
    pureCallee(t -> count++, s);
    // :: error: [purity.not.deterministic.object.creation]
    pureCallee(t -> new String("x").length(), s);
  }

  /** A @SideEffectFree callee constrains side effects only. */
  void lambdaArgumentsToSideEffectFreeCallee(String s) {
    sideEffectFreeCallee(t -> new String("x").length(), s);
    // :: error: [purity.not.sideeffectfree.assign.field]
    sideEffectFreeCallee(t -> count++, s);
  }

  /** A @Deterministic callee constrains determinism only. */
  void lambdaArgumentsToDeterministicCallee(String s) {
    deterministicCallee(t -> deterministicOnly(t), s);
    // :: error: [purity.not.deterministic.object.creation]
    deterministicCallee(t -> new String("x").length(), s);
    // The same lambda is not side-effect-free, so it is rejected for a @SideEffectFree callee.
    // :: error: [purity.not.sideeffectfree.call]
    sideEffectFreeCallee(t -> deterministicOnly(t), s);
  }

  // A method reference's annotations must cover the callee's.

  void methodReferenceArguments(String s) {
    pureCallee(this::pureMethod, s);
    // :: error: [purity.functional.argument]
    pureCallee(this::sideEffectFreeOnly, s);
    // :: error: [purity.functional.argument]
    pureCallee(this::deterministicOnly, s);
    sideEffectFreeCallee(this::sideEffectFreeOnly, s);
    deterministicCallee(this::deterministicOnly, s);
    // :: error: [purity.functional.argument]
    deterministicCallee(this::sideEffectFreeOnly, s);
  }
}
