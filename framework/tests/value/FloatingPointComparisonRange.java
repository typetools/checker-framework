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
