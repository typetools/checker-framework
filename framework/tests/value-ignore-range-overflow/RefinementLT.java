import org.checkerframework.common.value.qual.IntRange;

public class RefinementLT {

  void test_backwards(int a, int j, int s) {
    /** backwards less than */
    // :: error: (assignment.type.incompatible)
    @IntRange(from = 0) int aa = a;
    if (-1 < a) {
      @IntRange(from = 0) int b = a;
      @IntRange(from = -1) int b1 = a;
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 1) int b2 = a;
    } else {
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 0) int c = a;
    }

    if (0 < j) {
      @IntRange(from = 1) int k = j;
    } else {
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 1) int l = j;
    }

    if (1 < s) {
      @IntRange(from = 1) int t = s;
    } else {
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 1) int u = s;
    }
  }

  void test_forwards(int a, int j, int s) {
    /** forwards less than */
    // :: error: (assignment.type.incompatible)
    @IntRange(from = 0) int aa = a;
    if (a < -1) {
      // :: error: (assignment.type.incompatible)
      @IntRange(from = -1) int b = a;
    } else {
      @IntRange(from = -1) int c = a;
    }

    if (j < 0) {
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 0) int k = j;
    } else {
      @IntRange(from = 0) int l = j;
      @IntRange(from = -1) int l1 = j;
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 1) int l2 = j;
    }

    if (s < 1) {
      // :: error: (assignment.type.incompatible)
      @IntRange(from = 1) int t = s;
    } else {
      @IntRange(from = 1) int u = s;
    }
  }
}
