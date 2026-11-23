import org.checkerframework.checker.index.qual.LessThan;

// A comparison that is performed in floating point does not imply an integral bound.
class LessThanFloatComparison {

  // If `limit` is 16777216 and `i` is 16777217, then both round to 16777216.0f, so
  // `limit >= (f = i)` is true, but `i < limit + 1` is false.
  void rounding(int limit, int i) {
    float f;
    if (limit >= (f = i)) {
      // :: error: [assignment]
      @LessThan("limit + 1") int j = i;
    }
  }

  void roundingExample() {
    rounding(16777216, 16777217);
  }

  // If `d` is NaN, then `i > d` is false, but `d >= i` is false too.
  void nan(int i, double d) {
    if (i > d) {
    } else {
      // :: error: [assignment]
      @LessThan("d + 1") int j = i;
    }
  }
}
