import org.checkerframework.checker.index.qual.LessThan;

// If an operand of a floating-point comparison is NaN, then both the comparison and its inverse are
// false, so the else branch of the comparison implies nothing.
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

  void greaterThanBoxed(int j, Double d) {
    if (j >= d) {
    } else {
      // :: error: (assignment)
      @LessThan("d") int k = j;
    }
  }

  void lessThan(int j, int m) {
    if (j >= m) {
    } else {
      @LessThan("m") int k = j;
    }
  }
}
