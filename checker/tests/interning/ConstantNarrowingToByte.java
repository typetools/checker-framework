import org.checkerframework.checker.interning.qual.Interned;

// Assignment conversion narrows a constant of type char, short, or int to byte before boxing it
// (JLS 5.2), so the value is boxed by Byte.valueOf(), which returns a cached object.
public class ConstantNarrowingToByte {
  static final char C = 1;
  static final short S = 1;

  void narrowToByte() {
    @Interned Byte b1 = (char) 1;
    @Interned Byte b2 = (short) 1;
    @Interned Byte b3 = 1;
    @Interned Object o = b1;
  }

  void narrowConstantVariableToByte() {
    @Interned Byte b1 = C;
    @Interned Byte b2 = S;
    final char localC = 2;
    @Interned Byte b3 = localC;
    @Interned Byte b4 = 'a';
  }

  void narrowToShort() {
    // A boxed short is not interned.
    // :: error: [assignment]
    @Interned Short s = (char) 1;
  }

  void widenToShort() {
    // Assignment conversion widens a constant byte to short before boxing it, so the value is boxed
    // by Short.valueOf(), not by Byte.valueOf().
    Short s = (byte) 1;
    // :: error: [assignment]
    @Interned Object o = s;
  }
}
