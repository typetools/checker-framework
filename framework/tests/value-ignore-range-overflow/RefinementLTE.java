import org.checkerframework.common.value.qual.IntRange;

public class RefinementLTE {

  void test_backwards(int a, int j, int s) {
    /** backwards less than or equals */
    // :: error: (assignment)
    @IntRange(from = -1) int aa = a;
    if (-1 <= a) {
      @IntRange(from = -1) int b = a;
    } else {
      // :: error: (assignment)
      @IntRange(from = -1) int c = a;
    }

    if (0 <= j) {
      @IntRange(from = 0) int k = j;
      @IntRange(from = -1) int k1 = j;
      // :: error: (assignment)
      @IntRange(from = 1) int k2 = j;
    } else {
      // :: error: (assignment)
      @IntRange(from = 0) int l = j;
    }

    if (1 <= s) {
      @IntRange(from = 1) int t = s;
    } else {
      // :: error: (assignment)
      @IntRange(from = 1) int u = s;
    }
  }

  void test_forwards(int a, int j, int s) {
    /** forwards less than or equal */
    // :: error: (assignment)
    @IntRange(from = 0) int aa = a;
    if (a <= -1) {
      // :: error: (assignment)
      @IntRange(from = -1) int b0 = a;
      // :: error: (assignment)
      @IntRange(from = 0) int b = a;
      // :: error: (assignment)
      @IntRange(from = 1) int b2 = a;
    } else {
      @IntRange(from = 0) int c = a;
    }

    if (j <= 0) {
      // :: error: (assignment)
      @IntRange(from = 1) int k = j;
    } else {
      @IntRange(from = 1) int l = j;
    }

    if (s <= 1) {
      // :: error: (assignment)
      @IntRange(from = 1) int t = s;
    } else {
      @IntRange(from = 1) int u = s;
    }
  }
}
