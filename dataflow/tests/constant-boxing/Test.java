// Assignment conversion converts a compile-time constant to the primitive type of a Byte, Short,
// or Character variable before boxing it (JLS 5.2).  Switch expression results and return
// statements are assignment contexts.
public class Test {
  static final byte BYTE_CONSTANT = 1;

  public Byte test(int x) {
    Short s1 = (byte) 1;
    Short s2 = BYTE_CONSTANT;
    Byte b = (char) 1;
    Character c = (byte) 1;
    Byte b2 =
        switch (x) {
          case 0 -> 2;
          default -> (char) 3;
        };
    return 4;
  }
}
