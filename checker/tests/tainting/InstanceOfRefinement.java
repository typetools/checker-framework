// Test that `e instanceof @Untainted T` refines `e` when the `instanceof` is true, and only then.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.dataflow.qual.Pure;

public class InstanceOfRefinement {

  @Pure
  static @Tainted String getTainted() {
    return "hi";
  }

  static void isUntainted(@Untainted String a) {}

  void methodCall() {
    // :: warning: [instanceof.unsafe]
    if (getTainted() instanceof @Untainted String) {
      isUntainted(getTainted());
    }
  }

  void elseBranch(@Tainted String a) {
    // :: warning: [instanceof.unsafe]
    if (a instanceof @Untainted String) {
      isUntainted(a);
    } else {
      // :: error: [argument]
      isUntainted(a);
    }
  }

  void negated(@Tainted String a) {
    // :: warning: [instanceof.unsafe]
    if (!(a instanceof @Untainted String)) {
      // :: error: [argument]
      isUntainted(a);
    }
  }
}
