// Inference must retain a purity annotation that is written on a method with a
// functional-interface parameter, even though the annotation requires the arguments to that
// parameter to have the same purity.

import java.util.function.Function;
import java.util.function.Supplier;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.EnsuresQualifierIf;

public class PurityWrittenFunctionalParameter {

  Object obj;

  @SideEffectFree
  static Integer sideEffectFree(Function<Integer, Integer> f) {
    return f.apply(1);
  }

  @Deterministic
  static Integer deterministic(Function<Integer, Integer> f) {
    return f.apply(1);
  }

  @Pure
  static Integer pure(Function<Integer, Integer> f) {
    return f.apply(1);
  }

  @Pure
  static Object pureSupplier(Supplier<Object> s) {
    return s.get();
  }

  @SideEffectFree
  static void sideEffectFreeVoid(Runnable r) {
    r.run();
  }

  @SuppressWarnings("ainfertest")
  @EnsuresQualifierIf(expression = "#1", result = true, qualifier = AinferSibling1.class)
  boolean checkAinferSibling1(Object o) {
    return true;
  }

  // Each call below would unrefine the type of `obj` if inference weakened the written purity
  // annotation of the callee.
  @AinferSibling1 Object useSideEffectFree() {
    if (checkAinferSibling1(obj)) {
      sideEffectFree(x -> x);
      return obj;
    }
    return null;
  }

  @AinferSibling1 Object usePure() {
    if (checkAinferSibling1(obj)) {
      pure(x -> x);
      return obj;
    }
    return null;
  }

  @AinferSibling1 Object useSideEffectFreeVoid() {
    if (checkAinferSibling1(obj)) {
      sideEffectFreeVoid(() -> {});
      return obj;
    }
    return null;
  }
}
