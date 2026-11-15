// Inference of purity for methods that have a functional-interface parameter and that override,
// or are overridden by, another method.  Inference must not infer a purity annotation that would
// newly require something of the arguments at calls to a method, and it must not discard the
// purity that a method inherits.
//
// The validation run does not detect a violation of either rule:  inference of `I.m` itself
// yields @Impure, and that is the least upper bound of everything inferred for `I.m`; and
// `JImpl.m` inherits @SideEffectFree whatever is inferred for it.  To check these rules, inspect
// the inferred annotations on the methods named `m`.

import java.util.function.Function;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.EnsuresQualifierIf;

public class PurityInferenceFunctionalOverride {

  Object obj;

  int count;

  // `Impl.m` is written @Pure, but `I.m` has no annotation:  callers of `I.m` may pass any
  // argument.  Inference must not infer @Pure for `I.m` from `Impl.m`.
  interface I {
    Integer m(Function<Integer, Integer> f);
  }

  static class Impl implements I {
    @Pure
    @Override
    public Integer m(Function<Integer, Integer> f) {
      return 1;
    }
  }

  void callI(I i) {
    i.m(x -> count++);
  }

  // `JImpl.m` inherits @SideEffectFree from `J.m`, so its callers already must pass a
  // side-effect-free argument.  Inference must not infer @Impure for `JImpl.m`.
  interface J {
    @SideEffectFree
    Integer m(Function<Integer, Integer> f);
  }

  static class JImpl implements J {
    @Override
    public Integer m(Function<Integer, Integer> f) {
      return 1;
    }
  }

  @SuppressWarnings("ainfertest")
  @EnsuresQualifierIf(expression = "#1", result = true, qualifier = AinferSibling1.class)
  boolean checkAinferSibling1(Object o) {
    return true;
  }

  // The call below would unrefine the type of `obj` if inference made `JImpl.m` impure.
  @AinferSibling1 Object useJImpl(JImpl j) {
    if (checkAinferSibling1(obj)) {
      j.m(x -> x);
      return obj;
    }
    return null;
  }
}
