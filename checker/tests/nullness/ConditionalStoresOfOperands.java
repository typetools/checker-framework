// Test that an operator does not treat the facts that hold when one operand is true or false as
// facts that hold when the operator's result is true or false.

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ConditionalStoresOfOperands {

  @EnsuresNonNullIf(expression = "#1", result = true)
  static boolean isNN(@Nullable Object o) {
    return o != null;
  }

  @EnsuresNonNullIf(expression = "#1", result = false)
  static boolean isNull(@Nullable Object o) {
    return o == null;
  }

  void orLiteral(@Nullable Object o) {
    if (isNN(o) | true) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void orRight(@Nullable Object o, boolean b) {
    if (b | isNN(o)) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void orRightElse(@Nullable Object o, boolean b) {
    if (b | isNull(o)) {
    } else {
      o.hashCode();
    }
  }

  void andRight(@Nullable Object o, boolean b) {
    if (b & isNN(o)) {
      o.hashCode();
    } else {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void xorLiteral(@Nullable Object o) {
    if (isNN(o) ^ true) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void equalToVariable(@Nullable Object o, boolean b) {
    if (b == isNN(o)) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void notEqualToVariable(@Nullable Object o, boolean b) {
    if (b != isNN(o)) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void equalToComparison(@Nullable Object o) {
    if (isNN(o) == (2 < 1)) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void equalToString(@Nullable Object o) {
    if ("x" == (isNN(o) + "")) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void equalToNegatedTrue(@Nullable Object o) {
    if (isNN(o) == !true) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void equalToTrue(@Nullable Object o) {
    if (isNN(o) == true) {
      o.hashCode();
    }
  }

  void equalToFalse(@Nullable Object o) {
    if (isNN(o) == false) {
      return;
    }
    o.hashCode();
  }

  void equalToNegatedFalse(@Nullable Object o) {
    if (isNN(o) == !false) {
      o.hashCode();
    }
  }

  void notEqualToNegatedTrue(@Nullable Object o) {
    if (isNN(o) != !true) {
      o.hashCode();
    }
  }

  void trueEqualTo(@Nullable Object o) {
    if (true == isNN(o)) {
      o.hashCode();
    }
  }
}
