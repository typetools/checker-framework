// Assignment conversion converts a compile-time constant to the primitive type of a Byte, Short,
// or Character variable before boxing it (JLS 5.2).
public class Test {
  static final byte BYTE_CONSTANT = 1;

  public void test() {
    Short s1 = (byte) 1;
    Short s2 = BYTE_CONSTANT;
    Byte b = (char) 1;
    Character c = (byte) 1;
  }
}
