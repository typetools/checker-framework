// A relational comparison must not refine an operand that a later part of the comparison changes.
// For example, if i is 1, then i > (i = 0) is true, but afterward i is 0.

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class LowerBoundComparisonChangesOperand {

  void gtChangedLater(@NonNegative int i) {
    if (i > (i = 0)) {
      // :: error: (assignment)
      @Positive int p = i;
    }
  }

  void gtUnchanged(@NonNegative int i, int j) {
    if (i > (j = 0)) {
      @Positive int p = i;
    }
  }

  void gteChangedLater(int i) {
    if (i >= (i = -1) + 1) {
      // :: error: (assignment)
      @NonNegative int n = i;
    }
  }

  void gteUnchanged(int i, int m) {
    if (i >= (m = -1) + 1) {
      @NonNegative int n = i;
    }
  }

  // Both sides of an assignment have the value of the operand.
  void gtAssignment(int i, int j) {
    if ((i = j) > 0) {
      @Positive int p = i;
      @Positive int q = j;
    }
  }
}
