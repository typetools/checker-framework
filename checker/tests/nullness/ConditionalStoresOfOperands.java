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

  void andIsNull(@Nullable Object o, boolean b) {
    if (b & isNull(o)) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void trueAnd(@Nullable Object o) {
    if (true & isNN(o)) {
      o.hashCode();
    }
  }

  void andTrue(@Nullable Object o) {
    if (isNN(o) & true) {
      o.hashCode();
    }
  }

  void orFalse(@Nullable Object o) {
    if (isNN(o) | false) {
      o.hashCode();
    }
  }

  void xorFalse(@Nullable Object o) {
    if (isNN(o) ^ false) {
      o.hashCode();
    }
  }

  void xorTrueElse(@Nullable Object o) {
    if (isNN(o) ^ true) {
    } else {
      o.hashCode();
    }
  }

  void equalToCastTrue(@Nullable Object o) {
    if (isNN(o) == (boolean) true) {
      o.hashCode();
    }
  }

  void falseNotEqualTo(@Nullable Object o) {
    if (false != isNN(o)) {
      o.hashCode();
    }
  }

  void notEqualToFalse(@Nullable Object o) {
    if (isNN(o) != false) {
      o.hashCode();
    }
  }

  void instanceOfLambda(@Nullable Object o) {
    if (isNN(o) | !(((Runnable) () -> {}) instanceof Runnable)) {
    } else {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void instanceOfArrayCreation(@Nullable Object o) {
    if (isNN(o) | !(new Object[] {} instanceof Object[])) {
    } else {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void instanceOfStringConcatenation(@Nullable Object o, @Nullable Object p) {
    if (isNN(o) | !(("" + isNN(p)) instanceof String)) {
    } else {
      // :: error: [dereference.of.nullable]
      p.hashCode();
    }
  }

  void instanceOfInConditionalAnd(@Nullable Object o) {
    // :: error: [dereference.of.nullable]
    if (("" + isNN(o)) instanceof String && o.hashCode() == 0) {}
  }

  void conditionalAnd(@Nullable Object o, boolean b) {
    if (b && isNN(o)) {
      o.hashCode();
    }
  }

  void conditionalOrElse(@Nullable Object o, boolean b) {
    if (b || isNull(o)) {
    } else {
      o.hashCode();
    }
  }

  void castToBoolean(@Nullable Object o) {
    if ((boolean) isNN(o)) {
      o.hashCode();
    }
  }

  void ternary(@Nullable Object o, boolean b) {
    if (b ? isNN(o) : isNN(o)) {
      o.hashCode();
    }
  }

  @EnsuresNonNullIf(expression = "#1", result = true)
  static boolean returnsIsNN(@Nullable Object o) {
    return isNN(o);
  }

  @EnsuresNonNullIf(expression = "#1", result = true)
  static boolean returnsEqualToTrue(@Nullable Object o) {
    return isNN(o) == true;
  }

  @EnsuresNonNullIf(expression = "#1", result = true)
  static boolean returnsOrTrue(@Nullable Object o) {
    // :: error: [contracts.conditional.postcondition]
    return isNN(o) | true;
  }
}
