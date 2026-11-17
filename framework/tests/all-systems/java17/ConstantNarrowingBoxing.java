// Assignment conversion narrows a compile-time constant before boxing it (JLS 5.2), even if the
// constant is not a literal.

// @below-java17-jdk-skip-test

@SuppressWarnings("all") // Just check for crashes.
public class ConstantNarrowingBoxing {

  static final int CONSTANT = 3;

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
    byte b6 = (char) 1;
    short s3 = (char) 1;
    char c5 = (short) 1;
    char c6 = (byte) 1;
  }

  Byte switchResult(int n) {
    Byte b1 =
        switch (n) {
          case 0 -> 1;
          case 1 -> CONSTANT;
          default -> 1 + 2;
        };
    Character c1 =
        switch (n) {
          case 0 -> 'a';
          default -> 'a' + 1;
        };
    byte b2 =
        switch (n) {
          default -> 1 + 2;
        };
    return b1;
  }
}
