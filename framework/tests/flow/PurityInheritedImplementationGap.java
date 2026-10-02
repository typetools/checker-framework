// An inherited implementation that assumes the purity of a functional-interface argument, paired
// with an interface method that only a subclass implements with it, is not checked.

import java.util.function.Supplier;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityInheritedImplementationGap {
  static int count;

  static class Base {
    @SideEffectFree
    public int apply(Supplier<Integer> s) {
      return s.get();
    }
  }

  interface I<T> {
    @SideEffectFree
    int apply(T t);
  }

  /** Base.apply implements I<Supplier<Integer>>.apply, but only in Sub. */
  // :: error: [purity.functional.argument.parameter]
  static class Sub extends Base implements I<Supplier<Integer>> {}

  /** The parameter type at this call is T, so the argument is not checked. */
  @SideEffectFree
  static <T> int callThrough(I<T> i, T t) {
    return i.apply(t);
  }

  static int unannotated() {
    // callThrough is side-effect-free, yet this increments count.
    return callThrough(new Sub(), () -> count++);
  }
}
