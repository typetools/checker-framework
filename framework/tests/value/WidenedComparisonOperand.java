import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;

// Tests the refinement of an integral operand that a comparison widens to a floating-point type.
public class WidenedComparisonOperand {

  void intValLessThanDoubleVal(@IntVal({0, 1}) int i, @DoubleVal(0.5) double d) {
    if (i < d) {
      @IntVal(0) int j = i;
      // :: error: (assignment)
      @IntVal(42) int x = i;
    }
  }

  void intValEqualsFloat(@IntVal({5, 16777217}) int i) {
    float f = 16777216f;
    if (i == f) {
      // `(float) 16777217` is 16777216f.
      @IntVal(16777217) int j = i;
      // :: error: (assignment)
      @IntVal(42) int x = i;
    }
  }

  void intRangeEqualsFloat(@IntRange(from = 16777217, to = 16777300) int i) {
    float f = 16777216f;
    if (i == f) {
      // `(float) 16777217` is 16777216f.
      // :: error: (assignment)
      @IntVal(42) int x = i;
    }
  }

  void intRangeGreaterThanFloat(@IntRange(from = 0, to = 16777300) int i) {
    float f = 16777216f;
    if (i > f) {
    } else {
      // `i` might be 16777217, because `(float) 16777217` is 16777216f.
      // :: error: (assignment)
      @IntRange(from = 0, to = 16777216) int j = i;
    }
  }

  void longRangeEqualsDouble(@IntRange(from = 9007199254740993L, to = 9007199254741100L) long l) {
    double d = 9007199254740992.0;
    if (l == d) {
      // `(double) 9007199254740993L` is 9007199254740992.0.
      // :: error: (assignment)
      @IntVal(42) long x = l;
    }
  }

  void intRangeLessThanFloatExact(@IntRange(from = 0, to = 100) int i) {
    float f = 10.5f;
    if (i < f) {
      // Every value of `i` converts to float exactly.
      @IntRange(from = 0, to = 10) int j = i;
    }
  }

  void intRangeLessThanDouble(@IntRange(from = Integer.MIN_VALUE, to = 100) int i) {
    double d = 10.5;
    if (i < d) {
      // Every int converts to double exactly.
      @IntRange(from = Integer.MIN_VALUE, to = 10) int j = i;
    }
  }
}
