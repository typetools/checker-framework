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

  /** R's functional method is go, which is unrelated to Runnable.run. */
  interface R extends Runnable {
    @Override
    default void run() {
      count++;
    }

    @SideEffectFree
    void go();
  }

  /** Like R, but its implementation of Runnable.run is side-effect-free. */
  interface PureRun extends Runnable {
    @Override
    @SideEffectFree
    default void run() {}

    void go();
  }

  @SideEffectFree
  static void takesRunnable(Runnable r) {}

  /** A lambda cast to R implements R.go, but a call to r.run() runs R.run. */
  void castsCode() {
    // :: error: [purity.functional.argument]
    takesRunnable((R) () -> {});
    // :: error: [purity.functional.argument]
    takesRunnable((R) PurityFunctionalSubinterface::pureStatic);
    takesRunnable((PureRun) () -> {});
  }

  @SideEffectFree
  static void pureStatic() {}

  interface Takes<F extends Runnable> {
    @SideEffectFree
    void take(F f);
  }

  /**
   * A call through Takes checks its argument against Runnable.run, not against PureRun.go, so the
   * body may not assume that p.go() is side-effect-free.
   */
  static class TakesPureRun implements Takes<PureRun> {
    @Override
    @SideEffectFree
    public void take(PureRun p) {
      // :: error: [purity.call]
      p.go();
    }
  }

  /** A lambda passed to Takes<R>.take implements R.go, but the callee runs R.run. */
  void passesToInstantiation(Takes<R> t) {
    // :: error: [purity.functional.argument]
    t.take(() -> {});
  }
}
