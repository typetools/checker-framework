// Under -AassumeSideEffectFree, an argument to a functional-interface parameter is checked with
// the same assumption whatever its form:  a lambda body, a method reference, or the functional
// method of the argument's type.  Like PurityChecker, the assumption applies only to a method
// that has a purity annotation.

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

  /** A method with no purity annotation gets no assumption, in either form. */
  void unannotatedMethods(String s) {
    // :: error: [purity.not.sideeffectfree.call]
    callee(t -> unannotatedLength(t), s);
    // :: error: [purity.functional.argument]
    callee(this::unannotatedLength, s);
  }

  /** The assumption is that a called method has no side effect, not that this code has none. */
  void theAssumptionIsAboutCalls(String s) {
    // :: error: [purity.not.sideeffectfree.assign.field]
    callee(t -> count++, s);
  }
}
