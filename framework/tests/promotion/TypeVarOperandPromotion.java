// Numeric promotion unboxes an operand whose type is a type variable bounded by a primitive
// wrapper, as it unboxes an operand of the wrapper type.  See BinaryOperandPromotion.java for the
// meaning of the "promoted.left" and "promoted.right" errors.

import java.util.List;

public class TypeVarOperandPromotion {

  <T extends Integer> void unboxed(T a, int b) {
    // :: error: [promoted.left]
    int r = a + b;
  }

  <T extends Integer & Comparable<Integer>> void unboxedIntersection(T a, int b) {
    // :: error: [promoted.left]
    int r = a + b;
  }

  // The type of a.get(0) is a captured type variable whose upper bound is Integer.
  void unboxedCapturedWildcard(List<? extends Integer> a, int b) {
    // :: error: [promoted.left]
    int r = a.get(0) + b;
  }

  void unboxedCapturedBoolean(List<? extends Boolean> a, boolean b) {
    // :: error: [promoted.left]
    boolean r = a.get(0) & b;
  }

  // An equality test of two reference operands compares references and does not unbox them.
  <T extends Integer> void referenceEquality(T a, T b) {
    boolean r = a == b;
  }

  <T extends Integer> void numericEquality(T a, int b) {
    // :: error: [promoted.left]
    boolean r = a == b;
  }

  <T extends Integer> void shiftUnboxed(T a, long b) {
    // :: error: [promoted.left]
    int r = a << b;
  }
}
