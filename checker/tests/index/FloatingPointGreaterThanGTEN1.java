import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

// For a floating-point value x, `x > -1` does not imply `x >= 0`: x might be -0.5.
public class FloatingPointGreaterThanGTEN1 {

  void doubleGreaterThan(double d, @GTENegativeOne int limit) {
    if (d > limit) {
      // :: error: (assignment)
      @NonNegative double nn = d;
    }
  }

  void doubleAssignmentGreaterThan(double d, double input, @GTENegativeOne int limit) {
    if ((d = input) > limit) {
      // :: error: (assignment)
      @NonNegative double nn = d;
      // :: error: (assignment)
      @NonNegative double nn2 = input;
    }
  }

  void intGreaterThan(int i, @GTENegativeOne int limit) {
    if (i > limit) {
      @NonNegative int nn = i;
    }
  }

  // The comparison widens `i` to `double`, but `i` itself is an integer.
  void intGreaterThanDouble(int i, @GTENegativeOne double limit) {
    if (i > limit) {
      @NonNegative int nn = i;
    }
  }

  // For a floating-point value x, `x > 0` does not imply `x >= 1`: x might be 0.5.
  void doubleGreaterThanNonNegative(double d, @NonNegative int limit) {
    if (d > limit) {
      @NonNegative double nn = d;
      // :: error: (assignment)
      @Positive double pos = d;
    }
  }

  void castDoubleGreaterThanNonNegative(double d, @NonNegative int limit) {
    if ((double) d > limit) {
      @NonNegative double nn = d;
      // :: error: (assignment)
      @Positive double pos = d;
    }
  }

  void intGreaterThanNonNegative(int i, @NonNegative int limit) {
    if (i > limit) {
      @Positive int pos = i;
    }
  }

  void doubleNotEqualToMinusOne(@GTENegativeOne double d) {
    if (d != -1) {
      // :: error: (assignment)
      @NonNegative double nn = d;
    }
  }

  void castDoubleNotEqualToMinusOne(@GTENegativeOne double d) {
    if ((@GTENegativeOne double) d != -1) {
      // :: error: (assignment)
      @NonNegative double nn = d;
    }
  }

  void doubleNotEqualToZero(@NonNegative double d) {
    if (d != 0) {
      // :: error: (assignment)
      @Positive double pos = d;
    }
  }

  void intNotEqualToMinusOne(@GTENegativeOne int i) {
    if (i != -1) {
      @NonNegative int nn = i;
    }
  }

  void intAssignmentGreaterThan(int i, int input, @GTENegativeOne int limit) {
    if ((i = input) > limit) {
      @NonNegative int nn = i;
      @NonNegative int nn2 = input;
    }
  }
}
