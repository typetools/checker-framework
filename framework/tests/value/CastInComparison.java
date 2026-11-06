import org.checkerframework.common.value.qual.IntVal;

// Comparing a cast refines the cast's operand, if the cast does not change the value.
public class CastInComparison {

  void wideningCast(int i) {
    if ((long) i == 5L) {
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
}
