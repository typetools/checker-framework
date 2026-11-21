// Assignment conversion converts a compile-time constant to the primitive type of a Byte, Short, or
// Character variable before boxing it (JLS 5.2), even if the constant is not a literal.  Return
// statements, lambda bodies, and switch expression results are assignment contexts.

import java.util.function.Supplier;

@SuppressWarnings("all") // Just check for crashes.
public class ConstantNarrowingBoxing {

  static final int CONSTANT = 3;
  static final byte BYTE_CONSTANT = 1;

  void assignment() {
    Byte b1 = 1 + 2;
    Short s1 = (short) 1 + 2;
    Character c1 = 'a' + 1;
    Byte b2 = CONSTANT;
    Byte b3 = (1 + 2);
    byte b4 = 1 + 2;
    char c2 = CONSTANT;
    Byte b5 = (char) 1;
    Short s2 = (char) 1;
    Character c3 = (short) 1;
    Character c4 = (byte) 1;
    Short s4 = (byte) 1;
    Short s5 = BYTE_CONSTANT;
    byte b6 = (char) 1;
    short s3 = (char) 1;
    char c5 = (short) 1;
    char c6 = (byte) 1;
  }

  Byte returnConstant() {
    return 1;
  }

  Character returnByteConstant() {
    return BYTE_CONSTANT;
  }

  short returnCharConstant() {
    return 'a';
  }

  void lambdaBodies() {
    Supplier<Byte> s1 = () -> 1;
    Supplier<Short> s2 = () -> BYTE_CONSTANT;
    Supplier<Character> s3 =
        () -> {
          return CONSTANT;
        };
  }

  void switchResults(int x) {
    Byte b =
        switch (x) {
          case 0 -> 1;
          case 1 -> (char) 2;
          default -> {
            yield CONSTANT;
          }
        };
    short s =
        switch (x) {
          case 0 -> 'a';
          default -> BYTE_CONSTANT;
        };
  }
}
