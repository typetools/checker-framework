public class StringOffsetTest {
  public static void OffsetString() {
    char[] chars = new char[10];

    // :: error: (argument.type.incompatible)
    String string2 = new String(chars, 5, 7);

    String string3 = new String(chars, 5, 4);
  }
}
