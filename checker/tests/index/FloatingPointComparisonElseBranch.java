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

  void lessThan(int j, int m) {
    if (j >= m) {
    } else {
      @LessThan("m") int k = j;
    }
  }
}
