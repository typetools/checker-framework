// Unary numeric promotion unboxes a boxed operand before widening it, so a boxed operand of a shift
// is treated like the corresponding primitive.

import org.checkerframework.checker.signedness.qual.SignedPositive;

public class BoxedBinaryOperands {
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

  // A left shift can set the sign bit of the promoted int, as in '耀' << 16, so its result is
  // not @SignedPositive.
  @SignedPositive int primitiveCharLeftShift(char c) {
    // :: error: [return]
    return c << 16;
  }

  @SignedPositive int boxedCharLeftShift(Character c) {
    // :: error: [return]
    return c << 16;
  }
}
