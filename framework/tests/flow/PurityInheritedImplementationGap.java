// An inherited implementation that assumes the purity of a functional-interface argument, paired
// with an interface method that only a subclass implements with it, is checked at the subclass.
// A call through the interface method, whose parameter type is a type variable, checks nothing.

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
  // :: error: [purity.inherited.functional.parameter]
  static class Sub extends Base implements I<Supplier<Integer>> {}

  /** The error is reported only where the inherited method first implements I.apply. */
  static class SubOfSub extends Sub {}

  /** The parameter type at this call is T, which is inferred to be Supplier<Integer>. */
  @SideEffectFree
  static <T> int callThrough(I<T> i, T t) {
    return i.apply(t);
  }

  static int unannotated() {
    return callThrough(new Sub(), () -> count++);
  }
}
