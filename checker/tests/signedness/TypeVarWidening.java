import org.checkerframework.checker.signedness.qual.Unsigned;

// Assignment and casting conversions unbox an expression whose type is a type variable bounded by a
// primitive wrapper before they widen it, so such an expression is treated like the corresponding
// primitive.  Widening a char to int or long yields a non-negative signed value.
public class TypeVarWidening {
  <T extends Character> long assignToLong(T c) {
    return c;
  }

  <T extends Character & Comparable<Character>> long assignIntersectionToLong(T c) {
    return c;
  }

  <T extends @Unsigned Integer> long assignUnsignedToLong(T i) {
    // :: error: [return]
    return i;
  }

  <T extends Character> long castToLong(T c) {
    return (long) c;
  }

  <T extends Character> int castToInt(T c) {
    return (int) c;
  }

  <T extends Character & Comparable<Character>> long castIntersectionToLong(T c) {
    return (long) c;
  }

  <T extends @Unsigned Integer> long castUnsignedToLong(T i) {
    // :: error: [return]
    return (long) i;
  }

  // The same conversions, for a boxed expression.

  long boxedAssignToLong(Character c) {
    return c;
  }

  long boxedCastToLong(Character c) {
    return (long) c;
  }
}
