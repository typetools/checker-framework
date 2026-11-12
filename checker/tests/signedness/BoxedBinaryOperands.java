// Numeric promotion unboxes a boxed operand before widening it, so a boxed operand is treated like
// the corresponding primitive.

import org.checkerframework.checker.signedness.qual.SignedPositive;

public class BoxedBinaryOperands {
  int primitiveChar(char c) {
    return c / 2;
  }

  int boxedChar(Character c) {
    return c / 2;
  }

  long boxedCharWidenedToLong(Character c) {
    return c / 2L;
  }

  int boxedCharCompoundAssignment(int i, Character c) {
    i /= c;
    return i;
  }

  // A shift result takes the signedness of its left operand after unary numeric promotion, which
  // widens a char to an @SignedPositive int.
  @SignedPositive int primitiveCharShift(char c) {
    return c >> 2;
  }

  @SignedPositive int boxedCharShift(Character c) {
    return c >> 2;
  }

  @SignedPositive int boxedCharShiftByLong(Character c) {
    return c >> 2L;
  }

  // An equality test of a boxed operand and a primitive operand unboxes the boxed operand.
  boolean numericEquality(Character c, int i) {
    return c == i;
  }
}
