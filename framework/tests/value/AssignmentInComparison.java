import org.checkerframework.common.value.qual.IntVal;

// A comparison of an assignment refines only the sides that have the assigned value afterward.
public class AssignmentInComparison {

  static void localTarget(int i, int j) {
    if ((i = j) == 3) {
      @IntVal(3) int x = i;
      @IntVal(3) int y = j;
    }
  }

  static void indexReassigned(int i, int[] a) {
    if ((i = a[i]) == 3) {
      @IntVal(3) int x = i;
      // After the assignment, `a[i]` is a different element of `a`.
      // :: error: [assignment]
      @IntVal(3) int y = a[i];
    }
  }

  static void arrayTargetOtherArrayType(int[] a, long[] b, int[] idx) {
    // An assignment to an element of `b` does not change `idx[0]`, because `b` and `idx` cannot
    // alias.
    if ((b[0] = a[idx[0]]) == 3) {
      @IntVal(3) long x = b[0];
      @IntVal(3) int y = a[idx[0]];
    }
  }

  static void arrayTargetSameArrayType(int[] a, int[] b, int[] idx) {
    if ((b[0] = a[idx[0]]) == 3) {
      @IntVal(3) int x = b[0];
      // The assignment to `b[0]` might change `idx[0]`, if `b` and `idx` alias.
      // :: error: [assignment]
      @IntVal(3) int y = a[idx[0]];
    }
  }
}
