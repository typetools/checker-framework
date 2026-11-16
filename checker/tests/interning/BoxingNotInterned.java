import org.checkerframework.checker.interning.qual.Interned;

// A boxed value is not interned, even though every primitive is.  The exceptions are boolean and
// byte, for which boxing returns a cached object.
public class BoxingNotInterned {
  void box(int i) {
    // :: error: [assignment]
    @Interned Integer x = i;
    // :: error: [assignment]
    @Interned Object o = i;
    Integer y = i;
    // :: error: [assignment]
    @Interned Integer z = y;
  }

  void boxBooleanAndByte(boolean bool, byte b) {
    @Interned Boolean x = bool;
    @Interned Object o = bool;
    @Interned Byte y = b;
    @Interned Object p = b;
  }
}
