import org.checkerframework.common.value.qual.IntRange;

public class RefinementGT {

  void test_forward(int a, int j, int s) {
    /** forwards greater than */
    // :: error: (assignment.type.incompatible)
    @IntRange(from = 0) int aa = a;
    if (a > -1) {
      /** a is NN now */
      @IntRange(from = 0) int b = a;
      @IntRange(from = -1) int b1 = a;
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 1) int b2 = a;
    } else {
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 0) int c = a;
    }

    if (j > 0) {
      /** j is POS now */
      @IntRange(from = 1) int k = j;
    } else {
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 1) int l = j;
    }

    if (s > 1) {
      @IntRange(from = 1) int t = s;
    } else {
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 1) int u = s;
    }
  }

  void test_backwards(int a, int j, int s) {
    /** backwards greater than */
    // :: error: (assignment.type.incompatible)
    @IntRange(from = 0) int aa = a;
    if (-1 > a) {
      // :: error: (assignment.type.incompatible)
      @IntRange(from = -1) int b = a;
    } else {
      @IntRange(from = -1) int c = a;
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 0) int c1 = a;
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 1) int c2 = a;
    }

    if (0 > j) {
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 0) int k = j;
    } else {
      @IntRange(from = 0) int l = j;
    }

    if (1 > s) {
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 1) int t = s;
    } else {
      @IntRange(from = 1) int u = s;
    }
  }
}
