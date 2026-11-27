// Under -AassumeDeterministic, a body's call to a functional-interface parameter is deterministic
// whatever the argument is, so a method reference need not guarantee it.

import java.util.function.Function;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class AssumeDeterministicMethodRef {

  @SideEffectFree
  static int applySideEffectFree(Function<String, Integer> f, String s) {
    return f.apply(s);
  }

  @Deterministic
  static int applyDeterministic(Function<String, Integer> f, String s) {
    return f.apply(s);
  }

  @FunctionalInterface
  interface UnannotatedApplier {
    int apply(Function<String, Integer> f, String s);
  }

  void references() {
    // The assumption says nothing about side effects.
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier a = AssumeDeterministicMethodRef::applySideEffectFree;
    UnannotatedApplier b = AssumeDeterministicMethodRef::applyDeterministic;
  }
}
