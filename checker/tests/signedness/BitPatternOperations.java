// @skip-test

import org.checkerframework.checker.signedness.qual.BitPattern;

public class BitPatternOperations {

  void allowedUses(double d, float f, @BitPattern long bits, @BitPattern int pattern) {
    @BitPattern long fromDouble = Double.doubleToLongBits(d);
    @BitPattern long fromDoubleRaw = Double.doubleToRawLongBits(d);
    @BitPattern int fromFloat = Float.floatToIntBits(f);
    @BitPattern int fromFloatRaw = Float.floatToRawIntBits(f);

    @BitPattern long masked = bits & 0xFFFL;
    @BitPattern long combined = bits | fromDouble;
    @BitPattern long shifted = bits >>> 4;
    @BitPattern int shiftedPattern = pattern << 2;
    @BitPattern long complemented = ~bits;

    Double.longBitsToDouble(bits);
    Double.longBitsToDouble(masked | 1L);
    Float.intBitsToFloat(pattern);
    Float.intBitsToFloat(fromFloat);
  }

  void forbiddenUses(@BitPattern long bits, @BitPattern int pattern) {
    // :: error: (operation.bitpattern)
    long sum = bits + 1L;
    // :: error: (operation.bitpattern)
    long difference = 2L - bits;
    // :: error: (operation.bitpattern)
    int product = pattern * 3;
    // :: error: (compound.assignment.bitpattern)
    bits += 4L;
    // :: error: (compound.assignment.bitpattern)
    pattern -= 1;
    // :: error: (unary.bitpattern)
    bits++;
    // :: error: (unary.bitpattern)
    --pattern;
    // :: error: (bitpattern.concat)
    String s = bits + "";
  }
}
