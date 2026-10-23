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

  // In the tests below, a boolean operator whose other operand is a constant must not carry the
  // refinement into a branch where the instanceof may be false.

  void xorTrue(@Tainted String a) {
    // :: warning: [instanceof.unsafe]
    if ((a instanceof @Untainted String) ^ true) {
      // :: error: [argument]
      isUntainted(a);
    }
  }

  void andTrue(@Tainted String a) {
    // :: warning: [instanceof.unsafe]
    if ((a instanceof @Untainted String) & true) {
      isUntainted(a);
    } else {
      // :: error: [argument]
      isUntainted(a);
    }
  }

  void andFalse(@Tainted String a) {
    // :: warning: [instanceof.unsafe]
    if ((a instanceof @Untainted String) & false) {
    } else {
      // :: error: [argument]
      isUntainted(a);
    }
  }

  void orTrue(@Tainted String a) {
    // :: warning: [instanceof.unsafe]
    if ((a instanceof @Untainted String) | true) {
      // :: error: [argument]
      isUntainted(a);
    }
  }

  void equalsNotTrue(@Tainted String a) {
    // :: warning: [instanceof.unsafe]
    if ((a instanceof @Untainted String) == !true) {
      // :: error: [argument]
      isUntainted(a);
    } else {
      isUntainted(a);
    }
  }

  void equalsBoolean(@Tainted String a, @Tainted String b) {
    // :: warning: [instanceof.unsafe]
    if ((a instanceof @Untainted String) == (b instanceof @Untainted String)) {
      // :: error: [argument]
      isUntainted(b);
    }
  }
}
