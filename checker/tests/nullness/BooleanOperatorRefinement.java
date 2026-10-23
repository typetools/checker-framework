// Test that a boolean operator with a constant operand refines only where the refinement holds.

import org.checkerframework.checker.nullness.qual.Nullable;

public class BooleanOperatorRefinement {

  void andTrue(@Nullable Object x) {
    if ((x != null) & true) {
      x.toString();
    }
  }

  void andFalse(@Nullable Object x) {
    if ((x == null) & false) {
    } else {
      // :: error: [dereference.of.nullable]
      x.toString();
    }
  }

  void orTrue(@Nullable Object x) {
    if ((x != null) | true) {
      // :: error: [dereference.of.nullable]
      x.toString();
    }
  }

  void xorTrue(@Nullable Object x) {
    if ((x != null) ^ true) {
      // :: error: [dereference.of.nullable]
      x.toString();
    }
  }

  void equalsFalse(@Nullable Object x) {
    if ((x == null) == false) {
      x.toString();
    }
  }

  void equalsNotFalse(@Nullable Object x) {
    if ((x != null) == !false) {
      x.toString();
    }
  }

  void equalsNotTrue(@Nullable Object x) {
    if ((x != null) == !true) {
      // :: error: [dereference.of.nullable]
      x.toString();
    }
  }

  void equalsBoolean(@Nullable Object x, @Nullable Object y) {
    if ((x != null) == (y != null)) {
      // :: error: [dereference.of.nullable]
      y.toString();
    }
  }
}
