// The Promotion Checker reports "promoted.left" or "promoted.right" for each operand of a binary
// operation or compound assignment that is unboxed or widened.

public class BinaryOperandPromotion {

  void sameKinds(int a, int b) {
    int r = a + b;
  }

  void unboxedLeft(Integer a, int b) {
    // :: error: [promoted.left]
    int r = a + b;
  }

  void unboxedRight(int a, Integer b) {
    // :: error: [promoted.right]
    int r = a + b;
  }

  void unboxedBoth(Integer a, Integer b) {
    // :: error: [promoted.left] :: error: [promoted.right]
    int r = a + b;
  }

  void widenedLeft(int a, long b) {
    // :: error: [promoted.left]
    long r = a + b;
  }

  void widenedRight(long a, int b) {
    // :: error: [promoted.right]
    long r = a + b;
  }

  void widenedBoth(byte a, short b) {
    // :: error: [promoted.left] :: error: [promoted.right]
    int r = a + b;
  }

  // An equality test of two boxed operands compares references and does not unbox them.
  void referenceEquality(Integer a, Integer b) {
    boolean r = a == b;
  }

  void referenceInequality(Integer a, Integer b) {
    boolean r = a != b;
  }

  void numericEquality(Integer a, int b) {
    // :: error: [promoted.left]
    boolean r = a == b;
  }

  void numericInequality(int a, Integer b) {
    // :: error: [promoted.right]
    boolean r = a != b;
  }

  void booleanReferenceEquality(Boolean a, Boolean b) {
    boolean r = a == b;
  }

  void booleanSameKinds(boolean a, boolean b) {
    boolean r = a == b;
  }

  void booleanEquality(Boolean a, boolean b) {
    // :: error: [promoted.left]
    boolean r = a == b;
  }

  void booleanAnd(Boolean a, boolean b) {
    // :: error: [promoted.left]
    boolean r = a & b;
  }

  void booleanXorBoxed(Boolean a, Boolean b) {
    // :: error: [promoted.left] :: error: [promoted.right]
    boolean r = a ^ b;
  }

  void conditionalOr(boolean a, Boolean b) {
    // :: error: [promoted.right]
    boolean r = a || b;
  }

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

  void compoundUnboxed(int a, Integer b) {
    // :: error: [promoted.right]
    a /= b;
  }

  void compoundWidened(int a, long b) {
    // :: error: [promoted.left]
    a += b;
  }

  void compoundShiftByLong(int a, long b) {
    a <<= b;
  }

  void compoundShiftByByte(int a, byte b) {
    // :: error: [promoted.right]
    a >>= b;
  }
}
