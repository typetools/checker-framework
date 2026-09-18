// Under -AassumeSideEffectFree, an argument to a functional-interface parameter is checked with
// the same assumption whatever its form:  a lambda body, a method reference, or the functional
// method of the argument's type.  The assumption applies to every method, including one with no
// purity annotation, which is the case the option exists for:  an unannotated library.

import java.util.function.Function;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class AssumeSideEffectFreeArguments {

  int count = 0;

  @SideEffectFree
  static int callee(Function<String, Integer> f, String s) {
    return 0;
  }

  @Deterministic
  int deterministicLength(String s) {
    // The method's own body is still checked against its own annotation.
    // :: error: [purity.not.deterministic.assign.field]
    count++;
    return s.length();
  }

  int unannotatedLength(String s) {
    count++;
    return s.length();
  }

  /** A functional interface whose functional method promises determinism. */
  @FunctionalInterface
  interface DeterministicFunction extends Function<String, Integer> {
    @Override
    @Deterministic
    Integer apply(String s);
  }

  DeterministicFunction deterministicVariable = s -> s.length();

  /** The side effect of a method that promises determinism is assumed away. */
  void annotatedMethods(String s) {
    callee(t -> deterministicLength(t), s);
    callee(this::deterministicLength, s);
    callee(deterministicVariable, s);
  }

  /** A method with no purity annotation gets the assumption too. */
  void unannotatedMethods(String s) {
    // TODO: A lambda's body is checked by PurityChecker, which still applies an assumption only
    // to a method that has a purity annotation.  Remove this expectation once it does not.
    // :: error: [purity.not.sideeffectfree.call]
    callee(t -> unannotatedLength(t), s);
    callee(this::unannotatedLength, s);
  }

  /** A value of an unannotated functional-interface type gets the assumption too. */
  void unannotatedVariable(Function<String, Integer> f, String s) {
    callee(f, s);
  }

  /** The assumption is that a called method has no side effect, not that this code has none. */
  void theAssumptionIsAboutCalls(String s) {
    // :: error: [purity.not.sideeffectfree.assign.field]
    callee(t -> count++, s);
  }
}
