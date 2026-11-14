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
}
