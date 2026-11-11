import org.checkerframework.checker.signedness.qual.Unsigned;

// Binary numeric promotion unboxes a boxed operand before widening it, so a boxed operand is
// treated like the corresponding primitive.
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

  int primitiveUnsigned(@Unsigned int i) {
    // :: error: [operation.unsignedlhs] :: error: [return]
    return i / 2;
  }

  int boxedUnsigned(@Unsigned Integer i) {
    // :: error: [operation.unsignedlhs] :: error: [return]
    return i / 2;
  }

  // An equality test of two boxed operands compares references and does not unbox them.
  boolean referenceEquality(@Unsigned Integer u, Integer i) {
    // :: error: [comparison.mixed.unsignedlhs]
    return u == i;
  }

  boolean charReferenceEquality(Character c1, Character c2) {
    return c1 == c2;
  }

  // An equality test of a boxed operand and a primitive operand unboxes the boxed operand.
  boolean numericEquality(@Unsigned Integer u, int i) {
    // :: error: [comparison.mixed.unsignedlhs]
    return u == i;
  }
}
