import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

// Binary numeric promotion unboxes an operand whose type is a type variable bounded by a primitive
// wrapper, so such an operand is treated like the corresponding primitive.
public class TypeVarBinaryOperands {
  <T extends Character> int charDivide(T c) {
    return c / 2;
  }

  <T extends Character> long charDivideWidenedToLong(T c) {
    return c / 2L;
  }

  <T extends @Unsigned Integer> void unsignedDivide(T i) {
    // :: error: [operation.unsignedlhs]
    int quotient = i / 2;
  }

  <T extends @Unsigned Integer & Comparable<Integer>> void unsignedDivideIntersection(T i) {
    // :: error: [operation.unsignedlhs]
    int quotient = i / 2;
  }

  <T extends @Signed Integer, U extends @Unsigned Integer> void mixed(T t, U u) {
    // :: error: [operation.mixed.unsignedrhs]
    int sum = t + u;
  }

  <T extends @Unsigned Integer> void numericEquality(T u, @Signed int i) {
    // :: error: [comparison.mixed.unsignedlhs]
    boolean b = u == i;
  }

  // An equality test of two reference operands compares references and does not unbox them.
  <T extends @Unsigned Integer> boolean referenceEquality(T u, @Signed Integer s) {
    // :: error: [comparison.mixed.unsignedlhs]
    return u == s;
  }

  <T extends @Unsigned Integer> boolean referenceEqualityRhs(@Signed Integer s, T u) {
    // :: error: [comparison.mixed.unsignedrhs]
    return s != u;
  }

  <T extends @Unsigned Integer> void compoundDivide(int x, T t) {
    // :: error: [compound.assignment] :: error: [compound.assignment.unsigned.expression]
    x /= t;
  }
}
