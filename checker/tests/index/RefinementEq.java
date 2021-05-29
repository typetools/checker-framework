import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class RefinementEq {

  void test_equal(int a, int j, int s) {

    if (-1 == a) {
      @GTENegativeOne int b = a;
    } else {
      // :: error: (assignment)
      @GTENegativeOne int c = a;
    }

    if (0 == j) {
      @NonNegative int k = j;
    } else {
      // :: error: (assignment)
      @NonNegative int l = j;
    }

    if (1 == s) {
      @Positive int t = s;
    } else {
      // :: error: (assignment)
      @Positive int u = s;
    }
  }
}
// a comment
