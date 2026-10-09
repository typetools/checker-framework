// Soundness of the purity that a call relies on, when the functional arguments of the callee are
// checked only within code that is required to have that purity.

import java.util.function.Function;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.testchecker.util.*;

public class PurityFunctionalArgumentSoundness {

  static int counter = 0;

  String field;

  // A @SideEffectFree method assumes that its functional-interface parameter is side-effect-free,
  // even within a lambda that it returns.  A caller that is not required to be side-effect-free
  // may pass a side-effecting argument, and then the returned lambda is not side-effect-free.

  /** A functional interface whose functional method is side-effect-free. */
  @FunctionalInterface
  interface SideEffectFreeSupplier {
    @SideEffectFree
    int get();
  }

  @SideEffectFree
  static SideEffectFreeSupplier wrap(Function<Integer, Integer> f) {
    return () -> f.apply(1);
  }

  void escapingParameter(@Odd String p) {
    SideEffectFreeSupplier s =
        wrap(
            x -> {
              // :: error: [purity.assign.field]
              field = "";
              return 0;
            });
    field = p;
    // This call runs the lambda above, which modifies the field, but the error is above.
    s.get();
    @Odd String a = field;
  }

  // A side-effect-free method that returns no value is deterministic, but only if its functional
  // arguments are side-effect-free too.  A caller that is required only to be deterministic must
  // still check that.

  @SideEffectFree
  static void run(Runnable r) {
    r.run();
  }

  Runnable incrementer = () -> counter++;

  @Deterministic
  int deterministicCaller() {
    // :: error: [purity.functional.argument]
    run(incrementer);
    return counter;
  }
}
