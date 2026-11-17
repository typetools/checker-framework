// Under -AassumeSideEffectFree, a body's call to a functional-interface parameter is
// side-effect-free whatever the argument is, so a method reference need not guarantee it.

import java.util.function.Function;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class AssumeSideEffectFreeMethodRef {

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
    UnannotatedApplier a = AssumeSideEffectFreeMethodRef::applySideEffectFree;
    // The assumption says nothing about determinism.
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier b = AssumeSideEffectFreeMethodRef::applyDeterministic;
  }
}
