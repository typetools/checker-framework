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
  }
}
