import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntRange;

// If an operand of a floating-point comparison is NaN, then both the comparison and its inverse are
// false, so the else branch of the comparison implies nothing.
public class FloatingPointComparisonElseBranch {

  void greaterThan(@IntRange(from = 0, to = 100) int i, @DoubleVal({5.0, Double.NaN}) double d) {
    if (i > d) {
    } else {
      // `d` might be NaN, in which case `i > d` and `i <= d` are both false.
      // :: error: (assignment)
      @IntRange(from = 0, to = 5) int j = i;
    }
  }

  void greaterThanNaN(@IntRange(from = 0, to = 100) int i) {
    double d = Double.NaN;
    if (i > d) {
    } else {
      // :: error: (assignment)
      @IntRange(from = 0, to = 0) int j = i;
    }
  }

  void greaterThanOrEqual(
      @IntRange(from = 0, to = 100) int i, @DoubleVal({5.0, Double.NaN}) double d) {
    if (i >= d) {
    } else {
      // :: error: (assignment)
      @IntRange(from = 0, to = 4) int j = i;
    }
  }

  void lessThan(@IntRange(from = 0, to = 100) int i, @DoubleVal({5.0, Double.NaN}) double d) {
    if (d < i) {
    } else {
      // :: error: (assignment)
      @IntRange(from = 0, to = 5) int j = i;
    }
  }

  void lessThanOrEqual(
      @IntRange(from = 0, to = 100) int i, @DoubleVal({5.0, Double.NaN}) double d) {
    if (d <= i) {
    } else {
      // :: error: (assignment)
      @IntRange(from = 0, to = 4) int j = i;
    }
  }

  void greaterThanFloat(
      @IntRange(from = 0, to = 100) int i, @DoubleVal({5.0, Double.NaN}) float f) {
    if (i > f) {
    } else {
      // :: error: (assignment)
      @IntRange(from = 0, to = 5) int j = i;
    }
  }

  void greaterThanBoxed(
      @IntRange(from = 0, to = 100) int i, @DoubleVal({5.0, Double.NaN}) Double d) {
    if (i > d) {
    } else {
      // :: error: (assignment)
      @IntRange(from = 0, to = 5) int j = i;
    }
  }

  void intGreaterThan(@IntRange(from = 0, to = 100) int i, @IntRange(from = 0, to = 5) int m) {
    if (i > m) {
    } else {
      @IntRange(from = 0, to = 5) int j = i;
    }
  }
}
