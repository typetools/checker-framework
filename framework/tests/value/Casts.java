// Test case for https://github.com/typetools/checker-framework/issues/6141 .

import org.checkerframework.common.value.qual.IntVal;

public class Casts {

  final byte b = 65;
  final char c = 'A';
  final short s = 22222;
  final int i = 1234567890;
  final long l = 1234567890;
  final float f = 1e-14f;
  final double d = 1e-15;

  void testCastWithNoAnnotations() {
    byte b1 = (byte) b;
    byte b2 = (byte) c;
    byte b3 = (byte) s;
    byte b4 = (byte) i;
    byte b5 = (byte) l;
    byte b6 = (byte) f;
    byte b7 = (byte) d;

    char c1 = (char) b;
    char c2 = (char) c;
    char c3 = (char) s;
    char c4 = (char) i;
    char c5 = (char) l;
    char c6 = (char) f;
    char c7 = (char) d;

    short s1 = (short) b;
    short s2 = (short) c;
    short s3 = (short) s;
    short s4 = (short) i;
    short s5 = (short) l;
    short s6 = (short) f;
    short s7 = (short) d;

    int i1 = (int) b;
    int i2 = (int) c;
    int i3 = (int) s;
    int i4 = (int) i;
    int i5 = (int) l;
    int i6 = (int) f;
    int i7 = (int) d;

    long l1 = (long) b;
    long l2 = (long) c;
    long l3 = (long) s;
    long l4 = (long) i;
    long l5 = (long) l;
    long l6 = (long) f;
    long l7 = (long) d;

    float f1 = (float) b;
    float f2 = (float) c;
    float f3 = (float) s;
    float f4 = (float) i;
    float f5 = (float) l;
    float f6 = (float) f;
    float f7 = (float) d;

    double d1 = (double) b;
    double d2 = (double) c;
    double d3 = (double) s;
    double d4 = (double) i;
    double d5 = (double) l;
    double d6 = (double) f;
    double d7 = (double) d;
  }

  public void intCastTest1(@IntVal({0, 1}) int input) {
    @IntVal({0, 1}) int c = (int) input;
    @IntVal({0, 1}) int ac = (@IntVal({0, 1}) int) input;
    @IntVal({0, 1, 2}) int sc = (@IntVal({0, 1, 2}) int) input;
    // :: warning: (cast.unsafe)
    @IntVal({1}) int uc = (@IntVal({1}) int) input;
    // :: warning: (cast.unsafe)
    @IntVal({2}) int bc = (@IntVal({2}) int) input;
  }

  public void intCastTest2(@IntVal({2, 0}) int input) {
    @IntVal({0, 2}) int c = (int) input;
    @IntVal({0, 2}) int ac = (@IntVal({0, 2}) int) input;
    @IntVal({0, 1, 2}) int sc = (@IntVal({0, 1, 2}) int) input;
    // :: warning: (cast.unsafe)
    @IntVal({1}) int uc = (@IntVal({1}) int) input;
    // :: warning: (cast.unsafe)
    @IntVal({2}) int bc = (@IntVal({2}) int) input;
  }
}
