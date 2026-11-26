// The Promotion Checker reports "promoted.left" or "promoted.right" for each operand of a binary
// operation or compound assignment that is unboxed or widened.

public class BinaryOperandPromotion {

  // A shift operation promotes each operand separately, so a long right operand does not widen the
  // left operand.
  void shiftByLong(int a, long b) {
    int r = a << b;
  }

  void shiftUnboxed(Integer a, int b) {
    // :: error: [promoted.left]
    int r = a << b;
  }

  void shiftByte(byte a, int b) {
    // :: error: [promoted.left]
    int r = a << b;
  }

  void shiftChar(char a, long b) {
    // :: error: [promoted.left]
    int r = a >> b;
  }

  void shiftByByte(int a, byte b) {
    // :: error: [promoted.right]
    int r = a << b;
  }

  void shiftByBoxed(long a, Integer b) {
    // :: error: [promoted.right]
    long r = a >>> b;
  }

  void compoundShiftByLong(int a, long b) {
    a <<= b;
  }

  void compoundShiftByByte(int a, byte b) {
    // :: error: [promoted.right]
    a >>= b;
  }
}
