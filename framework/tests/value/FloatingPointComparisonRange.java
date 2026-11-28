import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntRange;

// Tests the conversion of the values of a floating-point operand of a comparison into a range of
// integers.
public class FloatingPointComparisonRange {

  void greaterThanNegativeFraction(
      @IntRange(from = -10, to = 10) int i, @DoubleVal(-0.5) double d) {
    if (i > d) {
      // `i` might be 0.
      // :: error: (assignment)
      @IntRange(from = 1, to = 10) int j = i;
      @IntRange(from = 0, to = 10) int k = i;
    }
  }

  void lessThanPositiveFraction(@IntRange(from = -10, to = 10) int i, @DoubleVal(0.5) double d) {
    if (i < d) {
      // `i` might be 0.
      // :: error: (assignment)
      @IntRange(from = -10, to = -1) int j = i;
      @IntRange(from = -10, to = 0) int k = i;
    }
  }

  void greaterThanOrEqualFraction(@IntRange(from = -10, to = 10) int i, @DoubleVal(2.5) double d) {
    if (i >= d) {
      @IntRange(from = 2, to = 10) int k = i;
    }
  }

  void notEqualNaN(@IntRange(from = 0, to = 10) int i) {
    double d = Double.NaN;
    if (i != d) {
      // `i != NaN` is true even if `i` is 0.
      // :: error: (assignment)
      @IntRange(from = 1, to = 10) int j = i;
    }
  }

  void equalNaN(@IntRange(from = 0, to = 10) int i) {
    double d = Double.NaN;
    if (i == d) {
    } else {
      // `i == NaN` is false even if `i` is 0.
      // :: error: (assignment)
      @IntRange(from = 1, to = 10) int j = i;
    }
  }

  void notEqualInteger(@IntRange(from = 0, to = 10) int i, @DoubleVal(0.0) double d) {
    if (i != d) {
      @IntRange(from = 1, to = 10) int j = i;
    }
  }

  void lessThanPositiveInfinity(@IntRange(from = 0, to = Long.MAX_VALUE) long l) {
    double d = Double.POSITIVE_INFINITY;
    if (l < d) {
      // `l` might be Long.MAX_VALUE.
      // :: error: (assignment)
      @IntRange(from = 0, to = Long.MAX_VALUE - 1) long m = l;
    }
  }

  void notEqualPositiveInfinity(@IntRange(from = 0, to = Long.MAX_VALUE) long l) {
    double d = Double.POSITIVE_INFINITY;
    if (l != d) {
      // `l` might be Long.MAX_VALUE.
      // :: error: (assignment)
      @IntRange(from = 0, to = Long.MAX_VALUE - 1) long m = l;
    }
  }

  void greaterThanNegativeInfinity(@IntRange(from = Long.MIN_VALUE, to = 0) long l) {
    double d = Double.NEGATIVE_INFINITY;
    if (l > d) {
      // `l` might be Long.MIN_VALUE.
      // :: error: (assignment)
      @IntRange(from = Long.MIN_VALUE + 1, to = 0) long m = l;
    }
  }

  void lessThanBeyondLong(
      @IntRange(from = 0, to = Long.MAX_VALUE) long l, @DoubleVal(1e19) double d) {
    if (l < d) {
      // `l` might be Long.MAX_VALUE.
      // :: error: (assignment)
      @IntRange(from = 0, to = Long.MAX_VALUE - 1) long m = l;
    }
  }

  void lessThanUnknown(@IntRange(from = 0, to = Long.MAX_VALUE) long l, double d) {
    if (l < d) {
      // `l` might be Long.MAX_VALUE, if `d` is infinite.
      // :: error: (assignment)
      @IntRange(from = 0, to = Long.MAX_VALUE - 1) long m = l;
    }
  }

  void greaterThanIntegralFloatingOperand(@IntRange(from = 0, to = 10) double d) {
    if (d > 5) {
      // Every value of `d` is an integer.
      @IntRange(from = 6, to = 10) double e = d;
    }
  }

  void lessThanFloatingOperand(@IntRange(from = 0, to = 100) int i, double d) {
    if (d < i) {
      if (d > 99.2) {
        // `d` might be 99.5.
        // :: error: (assignment)
        @DoubleVal(42.0) double e = d;
      }
    }
  }
}
