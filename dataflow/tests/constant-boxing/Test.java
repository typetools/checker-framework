// Assignment conversion converts a compile-time constant to the primitive type of a byte, short,
// char, Byte, Short, or Character variable, before boxing it if the variable's type is a boxed
// type (JLS 5.2).  Switch expression results and return statements are assignment contexts.
public class Test {
  static final byte BYTE_CONSTANT = 1;

  public Byte test(int x, short nonConstant) {
    Short s1 = (byte) 1;
    Short s2 = BYTE_CONSTANT;
    Byte b = (char) 1;
    Character c = (byte) 1;

    short s4 = 'a';
    byte b3 = 'a';
    char c2 = (byte) 1;

    // A value that is not a compile-time constant is only boxed, with no narrowing.
    Short s5 = nonConstant;

    Byte b2 =
        switch (x) {
          case 0 -> 2;
          default -> (char) 3;
        };
    Short s3 =
        switch (x) {
          case 0 -> (byte) 1;
          default -> 2;
        };
    return 4;
  }
}
