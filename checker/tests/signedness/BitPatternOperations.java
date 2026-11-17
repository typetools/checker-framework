import org.checkerframework.checker.signedness.qual.BitPattern;
import org.checkerframework.checker.signedness.qual.Unsigned;

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
    // :: error: [operation.bitpattern]
    long sum = bits + 1L;
    // :: error: [operation.bitpattern]
    long difference = 2L - bits;
    // :: error: [operation.bitpattern]
    int product = pattern * 3;
    // :: error: [compound.assignment.bitpattern]
    bits += 4L;
    // :: error: [compound.assignment.bitpattern]
    pattern -= 1;
    // :: error: [unary.bitpattern]
    bits++;
    // :: error: [unary.bitpattern]
    --pattern;
    // :: error: [bitpattern.concat]
    String s = bits + "";
  }

  void allowedComparisons(double d1, double d2, @BitPattern long bits, @BitPattern long other) {
    // Exact comparison of two doubles, which is what Double.equals does.
    boolean sameBits = Double.doubleToLongBits(d1) == Double.doubleToLongBits(d2);
    boolean differentBits = bits != other;
    // Testing which bits are set, against a manifest constant.
    boolean anyLowBitSet = (bits & 0xFFFL) != 0L;
    boolean isExactly = bits == 0x7FF0000000000000L;
  }

  void forbiddenComparisons(@BitPattern long bits, long signedValue) {
    // Ordering a bit pattern is meaningless: it is not an arithmetic quantity.
    // :: error: [operation.bitpattern]
    boolean lt = bits < 0L;
    // :: error: [operation.bitpattern]
    boolean ge = bits >= 1L;
    // Comparing a bit pattern against a value with a signedness interpretation.
    // :: error: [comparison.mixed.bitpattern]
    boolean mixedEquality = bits == signedValue;
  }

  void widening(@BitPattern long bits) {
    // Promotion to a floating-point type must not escape detection: binary numeric
    // promotion replaces @BitPattern with @Signed, which would otherwise hide these.
    // :: error: [operation.bitpattern]
    boolean ordered = bits < 1.0;
    // :: error: [operation.bitpattern]
    double sum = bits + 1.0;
  }

  void compoundWidening(@BitPattern long bits) {
    // Java permits `long += double` via implicit narrowing, which also promotes to double.
    // :: error: [compound.assignment.bitpattern]
    bits += 1.0;
  }

  void shiftCountIsBitPattern(@Unsigned int value, @BitPattern int count) {
    // A @BitPattern shift count does not change the interpretation of the value being
    // shifted, so the ordinary signed-right-shift-of-@Unsigned check still applies.
    // :: error: [shift.signed]
    int x = value >> count;
    // :: error: [compound.assignment.shift.signed]
    value >>= count;
  }

  void maskedShift(@BitPattern long bits) {
    // A masked shift remains a @BitPattern value: it must not be reinterpreted as
    // @SignedPositive by the heuristic that recognizes `(x >> n) & mask` as non-negative.
    @BitPattern long y = (bits >>> 4) & 0xFFL;
  }
}
