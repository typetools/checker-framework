import org.checkerframework.checker.interning.qual.Interned;

// Every boxing conversion of a constant from -128 to 127, or of a char constant from '\u0000' to
// '\u007f', yields the same object (JLS 5.1.7).
public class CachedBoxedConstants {
  static final int CACHED = 127;
  static final int UNCACHED = 128;

  void autoboxing() {
    @Interned Integer i1 = 5;
    @Interned Integer i2 = -128;
    @Interned Integer i3 = 127;
    @Interned Integer i4 = CACHED;
    @Interned Integer i5 = 100 + 27;
    // :: error: [assignment]
    @Interned Integer i6 = 128;
    // :: error: [assignment]
    @Interned Integer i7 = -129;
    // :: error: [assignment]
    @Interned Integer i8 = UNCACHED;

    @Interned Short s1 = 127;
    // :: error: [assignment]
    @Interned Short s2 = 128;

    @Interned Long l1 = -128L;
    // :: error: [assignment]
    @Interned Long l2 = 128L;

    @Interned Character c1 = '\u007f';
    // :: error: [assignment]
    @Interned Character c2 = '\u0080';

    @Interned Object o1 = 5;
    // :: error: [assignment]
    @Interned Object o2 = 1000;
    // :: error: [assignment]
    @Interned Double d = 1.0;
  }

  void nonConstant(int i) {
    // :: error: [assignment]
    @Interned Integer boxed = i;
  }

  void valueOf(int i) {
    @Interned Integer i1 = Integer.valueOf(5);
    @Interned Short s1 = Short.valueOf((short) 5);
    @Interned Long l1 = Long.valueOf(5);
    @Interned Character c1 = Character.valueOf('a');
    // :: error: [assignment]
    @Interned Integer i2 = Integer.valueOf(1000);
    // :: error: [assignment]
    @Interned Integer i3 = Integer.valueOf(i);
    // :: error: [assignment]
    @Interned Integer i4 = Integer.valueOf("5");
  }

  void dataflow() {
    Integer cached = 5;
    @Interned Object o1 = cached;
    Integer uncached = 1000;
    // :: error: [assignment]
    @Interned Object o2 = uncached;
  }

  void argument() {
    takesInterned(5);
    // :: error: [argument]
    takesInterned(1000);
  }

  @Interned Integer returned() {
    return 5;
  }

  void takesInterned(@Interned Integer i) {}
}
