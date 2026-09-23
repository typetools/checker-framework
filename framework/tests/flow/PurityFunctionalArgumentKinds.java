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
    // :: error: [purity.assign.field]
    pureCallee(t -> count++, s);
    // :: error: [purity.object.creation]
    pureCallee(t -> new String("x").length(), s);
  }

  /** A @SideEffectFree callee constrains side effects only. */
  void lambdaArgumentsToSideEffectFreeCallee(String s) {
    sideEffectFreeCallee(t -> new String("x").length(), s);
    // :: error: [purity.assign.field]
    sideEffectFreeCallee(t -> count++, s);
  }

  /** A @Deterministic callee constrains determinism only. */
  void lambdaArgumentsToDeterministicCallee(String s) {
    deterministicCallee(t -> deterministicOnly(t), s);
    // :: error: [purity.object.creation]
    deterministicCallee(t -> new String("x").length(), s);
    // The same lambda is not side-effect-free, so it is rejected for a @SideEffectFree callee.
    // :: error: [purity.call]
    sideEffectFreeCallee(t -> deterministicOnly(t), s);
  }

  // A side-effect-free functional method that returns no value is deterministic, whatever
  // implements it, so a @Pure callee requires only side-effect-freeness of it.

  @Pure
  static int pureRunnableCallee(Runnable r) {
    return 0;
  }

  @SideEffectFree
  void sideEffectFreeVoid() {}

  @SideEffectFree
  int sideEffectFreeNotDeterministic() {
    return count;
  }

  void impureVoid() {
    count++;
  }

  void voidFunctionalMethod() {
    pureRunnableCallee(() -> sideEffectFreeVoid());
    pureRunnableCallee(this::sideEffectFreeVoid);
    // The lambda is not deterministic, but as a side-effect-free void callback it is.
    pureRunnableCallee(() -> sideEffectFreeNotDeterministic());
    // :: error: [purity.call]
    pureRunnableCallee(() -> impureVoid());
    // :: error: [purity.functional.argument]
    pureRunnableCallee(this::impureVoid);
  }

  // A @Deterministic callee does not require side-effect-freeness of its functional arguments, so
  // determinism is still required of a void callback:  the callback's side effects could make a
  // later call to the callee compute a different value.

  @Deterministic
  static int deterministicRunnableCallee(Runnable r) {
    return 0;
  }

  void voidFunctionalMethodDeterministicCallee() {
    deterministicRunnableCallee(() -> sideEffectFreeVoid());
    deterministicRunnableCallee(this::sideEffectFreeVoid);
    // :: error: [purity.call]
    deterministicRunnableCallee(() -> impureVoid());
    // :: error: [purity.functional.argument]
    deterministicRunnableCallee(this::impureVoid);
    // :: error: [purity.assign.field]
    deterministicRunnableCallee(() -> count++);
    // Conservative:  the callback discards the value, so a side-effect-free callback is
    // deterministic, but the requirement is checked against the code that the argument denotes.
    // :: error: [purity.call]
    deterministicRunnableCallee(() -> sideEffectFreeNotDeterministic());
    // :: error: [purity.functional.argument]
    deterministicRunnableCallee(this::sideEffectFreeNotDeterministic);
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
