import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class RefinementGTE {

  void test_forward(int a, int j, int s) {
    /** forwards greater than or equals */
    // :: error: (assignment)
    @GTENegativeOne int aa = a;
    if (a >= -1) {
      @GTENegativeOne int b = a;
    } else {
      // :: error: (assignment)
      @GTENegativeOne int c = a;
    }

    if (j >= 0) {
      @NonNegative int k = j;
    } else {
      // :: error: (assignment)
      @NonNegative int l = j;
    }
  }

  void test_backwards(int a, int j, int s) {
    /** backwards greater than or equal */
    // :: error: (assignment)
    @NonNegative int aa = a;
    if (-1 >= a) {
      // :: error: (assignment)
      @NonNegative int b = a;
    } else {
      @NonNegative int c = a;
    }

    if (0 >= j) {
      // :: error: (assignment)
      @Positive int k = j;
    } else {
      @Positive int l = j;
    }

    if (1 >= s) {
      // :: error: (assignment)
      @Positive int t = s;
    } else {
      @Positive int u = s;
    }
  }
}
// a comment
