// Each result of a switch expression is converted to the switch expression's type, and a
// compile-time constant result is narrowed before it is boxed (JLS 5.2).

// @below-java17-jdk-skip-test

@SuppressWarnings("all") // Just check for crashes.
public class ConstantNarrowingBoxingInSwitch {

  static final int CONSTANT = 3;

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
