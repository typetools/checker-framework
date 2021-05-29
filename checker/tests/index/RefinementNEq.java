import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class RefinementNEq {

  void test_not_equal(int a, int j, int s) {

    // :: error: (assignment)
    @NonNegative int aa = a;
    if (-1 != a) {
      // :: error: (assignment)
      @GTENegativeOne int b = a;
    } else {
      @GTENegativeOne int c = a;
    }

    if (0 != j) {
      // :: error: (assignment)
      @NonNegative int k = j;
    } else {
      @NonNegative int l = j;
    }

    if (1 != s) {
      // :: error: (assignment)
      @Positive int t = s;
    } else {
      @Positive int u = s;
    }
  }
}
// a comment
