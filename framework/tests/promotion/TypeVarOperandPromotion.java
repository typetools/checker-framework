// Numeric promotion unboxes an operand whose type is a type variable bounded by a primitive
// wrapper,
// as it unboxes an operand of the wrapper type.  See BinaryOperandPromotion.java for the meaning of
// the "return" errors.

public class TypeVarOperandPromotion {

  <T extends Integer> int unboxed(T a, int b) {
    // :: error: [return]
    return a + b;
  }

  <T extends Integer & Comparable<Integer>> int unboxedIntersection(T a, int b) {
    // :: error: [return]
    return a + b;
  }

  // An equality test of two reference operands compares references and does not unbox them.
  <T extends Integer> boolean referenceEquality(T a, T b) {
    return a == b;
  }

  <T extends Integer> boolean numericEquality(T a, int b) {
    // :: error: [return]
    return a == b;
  }

  <T extends Integer> int shiftUnboxed(T a, long b) {
    // :: error: [return]
    return a << b;
  }
}
