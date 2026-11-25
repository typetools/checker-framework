import org.checkerframework.checker.index.qual.LessThan;

// An operand of a floating-point comparison might be NaN. If an operand is NaN, then both the
// comparison and its inverse are false, so the else branch of a floating-point comparison does not
// imply the inverse comparison.
public class FloatingPointComparisonElseBranch {

  void greaterThanOrEqual(int j, double d) {
    if (j >= d) {
    } else {
      // :: error: (assignment)
      @LessThan("d") int k = j;
    }
  }

  void greaterThanOrEqualAssignment(int i, int j, double d) {
    if ((i = j) >= d) {
    } else {
      // :: error: (assignment)
      @LessThan("d") int k = j;
    }
  }

  void greaterThan(int i, double bound) {
    if (i > bound) {
    } else {
      // `bound` might be NaN, in which case `i > bound` and `i <= bound` are both false.
      // :: error: (assignment)
      @LessThan("bound + 1") int y = i;
    }
  }

  void greaterThanOrEqualBoxed(int j, Double d) {
    if (j >= d) {
    } else {
      // :: error: (assignment)
      @LessThan("d") int k = j;
    }
  }

  void greaterThanOrEqualFloat(int j, float f) {
    if (j >= f) {
    } else {
      // :: error: (assignment)
      @LessThan("f") int k = j;
    }
  }

  void lessThan(int j, double d) {
    if (d < j) {
    } else {
      // `d` might be NaN, in which case `d < j` and `d >= j` are both false.
      // :: error: (assignment)
      @LessThan("d + 1") int k = j;
    }
  }

  void lessThanOrEqual(int j, double d) {
    if (d <= j) {
    } else {
      // :: error: (assignment)
      @LessThan("d") int k = j;
    }
  }

  void intGreaterThanOrEqual(int j, int m) {
    if (j >= m) {
    } else {
      @LessThan("m") int k = j;
    }
  }
}
