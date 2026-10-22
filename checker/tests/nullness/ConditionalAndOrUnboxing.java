import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

// The operands of && and || are unboxed, so a Boolean operand is non-null after it is evaluated.
public class ConditionalAndOrUnboxing {
  void leftOfAnd(@Nullable Boolean b, boolean x) {
    // :: error: [unboxing.of.nullable]
    if (b && x) {}
    b.toString();
  }

  void leftOfOr(@Nullable Boolean b, boolean x) {
    // :: error: [unboxing.of.nullable]
    if (b || x) {}
    b.toString();
  }

  void rightOfAnd(boolean x, @Nullable Boolean b) {
    // :: error: [unboxing.of.nullable]
    if (x && b) {
      b.toString();
    }
    // The right operand is not evaluated when x is false.
    // :: error: [dereference.of.nullable]
    b.toString();
  }

  void rightOfOr(boolean x, @Nullable Boolean b) {
    // :: error: [unboxing.of.nullable]
    if (x || b) {
    } else {
      b.toString();
    }
    // The right operand is not evaluated when x is true.
    // :: error: [dereference.of.nullable]
    b.toString();
  }

  // Unboxing an operand keeps the facts that hold when the operand is true or false.

  @EnsuresNonNullIf(expression = "#1", result = true)
  static Boolean isNN(@Nullable Object o) {
    return o != null;
  }

  @EnsuresNonNullIf(expression = "#1", result = false)
  static Boolean isNull(@Nullable Object o) {
    return o == null;
  }

  boolean refinedLeftOfAnd(@Nullable Object o) {
    return isNN(o) && o.hashCode() > 0;
  }

  boolean refinedLeftOfOr(@Nullable Object o) {
    return isNull(o) || o.hashCode() > 0;
  }

  boolean refinedCastLeftOfAnd(@Nullable Object o) {
    return ((Boolean) (o != null)) && o.hashCode() > 0;
  }

  void refinedRightOfAnd(boolean x, @Nullable Object o) {
    if (x && isNN(o)) {
      o.hashCode();
    }
  }

  void refinedRightOfOr(boolean x, @Nullable Object o) {
    if (x || isNull(o)) {
    } else {
      o.hashCode();
    }
  }
}
