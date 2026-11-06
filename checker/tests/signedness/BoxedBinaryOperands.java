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
}
