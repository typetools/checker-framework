import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.IntVal;

// Comparing a cast refines the cast's operand, if the cast does not change the value.
public class CastInComparison {

  void wideningCast(int i) {
    if ((long) i == 5L) {
      @IntVal(5) int j = i;
    }
  }

  // Binary numeric promotion widens the cast to long, which does not change its value.
  void promotedCast(short s) {
    if ((int) s == 5L) {
      @IntVal(5) short t = s;
    }
  }

  // Binary numeric promotion widens the cast to float, which might change its value.
  void lossilyPromotedCast(long l) {
    if ((long) l == 5f) {
      // :: error: [assignment]
      @IntVal(5) long m = l;
    }
  }

  // The refinement of the cast is a @DoubleVal, which is converted to an @IntVal for `i`.
  void wideningCastToDouble(@IntVal({5, 6}) int i) {
    if ((double) i == 5.0) {
      @IntVal(5) int j = i;
      // :: error: [assignment]
      @IntVal(99) int w = i;
    }
  }

  // No int value equals 5.5, so the then branch is unreachable.  The value 5.5 is not truncated
  // to 5.
  void wideningCastToDoubleNonIntegral(@IntVal({5, 6}) int i) {
    if ((double) i == 5.5) {
      @IntVal(99) int w = i;
    } else {
      // :: error: [assignment]
      @IntVal(5) int j = i;
    }
  }

  void narrowingCast(int i) {
    if ((byte) i == 5) {
      // :: error: [assignment]
      @IntVal(5) int j = i;
    }
  }

  // Unlike in `narrowingCast`, there is no binary numeric promotion of the cast.
  void narrowingCastNoPromotion(long l) {
    if ((int) l == 5) {
      // :: error: [assignment]
      @IntVal(5) long m = l;
    }
  }

  void lossyWideningCast(long l) {
    if ((float) l == 5f) {
      // :: error: [assignment]
      @IntVal(5) long m = l;
    }
  }

  // The assignment converts `l` to double, which might change its value.
  void lossyAssignmentConversionInCast(double d, long l) {
    if (((double) (d = l)) == 5d) {
      // :: error: [assignment]
      @IntVal(5) long m = l;
    }
  }

  // Comparing a cast of an array length refines the array.
  void castArrayLength(int[] a) {
    if ((long) a.length == 5) {
      @ArrayLen(5) int[] b = a;
    }
  }

  // Comparing a cast of a string length refines the string.
  void castStringLength(String s) {
    if ((long) s.length() == 5) {
      @ArrayLen(5) String t = s;
    }
  }

  static class Base {
    static int f;
  }

  static class Left extends Base {}

  static class Right extends Base {}

  // `Left.f` and `Right.f` are the same field, so assigning `Right.f` changes `Left.f`.
  void staticFieldThroughUnrelatedClasses() {
    Base.f = 1;
    if (((long) Left.f) == (Right.f = 0) + 1) {
      // :: error: [assignment]
      @IntVal(1) int wrong = Left.f;
    }
  }
}
