// Assignment conversion converts a compile-time constant to the primitive type of a byte, short,
// char, Byte, Short, or Character variable, before boxing it if the variable's type is a boxed
// type (JLS 5.2).
public class Test {
  static final byte BYTE_CONSTANT = 1;

  public void test(short nonConstant) {
    Short s1 = (byte) 1;
    Short s2 = BYTE_CONSTANT;
    Byte b = (char) 1;
    Character c = (byte) 1;

    short s3 = 'a';
    byte b2 = 'a';
    char c2 = (byte) 1;

    // A value that is not a compile-time constant is only boxed, with no narrowing.
    Short s4 = nonConstant;
  }
}
