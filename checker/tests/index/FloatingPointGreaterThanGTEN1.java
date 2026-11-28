import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;

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

  void intAssignmentGreaterThan(int i, int input, @GTENegativeOne int limit) {
    if ((i = input) > limit) {
      @NonNegative int nn = i;
      @NonNegative int nn2 = input;
    }
  }
}
