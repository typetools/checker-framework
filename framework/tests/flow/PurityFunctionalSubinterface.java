// A functional subinterface may implement a functional method of its superinterface with a default
// method, and declare an unrelated abstract method as its own functional method. An argument of
// the subinterface's type runs the default method, so it is checked against that method.

import java.util.function.Function;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityFunctionalSubinterface {

  static int count = 0;

  /** G's functional method is other, which is unrelated to Function.apply. */
  interface G extends Function<String, Integer> {
    @Override
    default Integer apply(String s) {
      return count++;
    }

    @SideEffectFree
    int other();
  }

  /** Like G, but its implementation of Function.apply is side-effect-free. */
  interface PureApply extends Function<String, Integer> {
    @Override
    @SideEffectFree
    default Integer apply(String s) {
      return 0;
    }

    int other();
  }

  @SideEffectFree
  static int m(Function<String, Integer> f, String s) {
    return f.apply(s);
  }

  void passes(G g, PureApply p) {
    // :: error: [purity.functional.argument]
    m(g, "");
    m(p, "");
  }

  /** The caller of forwards checked the code that g.other runs, not the code that g.apply runs. */
  @SideEffectFree
  static int forwards(G g, String s) {
    // :: error: [purity.functional.argument]
    return m(g, s);
  }
}
