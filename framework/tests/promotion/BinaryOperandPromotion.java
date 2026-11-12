// The Promotion Checker gives type @Promoted to an operand that is unboxed or widened.  The type of
// a binary operation is the least upper bound of the types of its operands, so a method that
// returns a binary operation has a "return" error if the operation unboxes or widens an operand.

public class BinaryOperandPromotion {

  int sameKinds(int a, int b) {
    return a + b;
  }

  int unboxed(Integer a, int b) {
    // :: error: [return]
    return a + b;
  }

  long widened(int a, long b) {
    // :: error: [return]
    return a + b;
  }

  // An equality test of two boxed operands compares references and does not unbox them.
  boolean referenceEquality(Integer a, Integer b) {
    return a == b;
  }

  boolean numericEquality(Integer a, int b) {
    // :: error: [return]
    return a == b;
  }

  boolean booleanReferenceEquality(Boolean a, Boolean b) {
    return a == b;
  }

  boolean booleanSameKinds(boolean a, boolean b) {
    return a == b;
  }

  boolean booleanEquality(Boolean a, boolean b) {
    // :: error: [return]
    return a == b;
  }

  boolean booleanAnd(Boolean a, boolean b) {
    // :: error: [return]
    return a & b;
  }

  boolean booleanXorBoxed(Boolean a, Boolean b) {
    // :: error: [return]
    return a ^ b;
  }

  boolean conditionalOr(boolean a, Boolean b) {
    // :: error: [return]
    return a || b;
  }

  // A shift operation promotes each operand separately, so a long right operand does not widen the
  // left operand.
  int shiftByLong(int a, long b) {
    return a << b;
  }

  int shiftUnboxed(Integer a, int b) {
    // :: error: [return]
    return a << b;
  }

  int shiftByte(byte a, int b) {
    // :: error: [return]
    return a << b;
  }
}
