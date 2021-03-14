// Test case for https://github.com/kelloggm/checker-framework/issues/158
// It is easy to see that:
//   * i is an index for intermediate
//   * length <= i (or, at least length <= i+1)
// but I don't see how to verify that length is an index for intermediate.

// @skip-test

import org.checkerframework.checker.index.qual.IndexOrHigh;

public class VarLteVar {

  /** Returns an array that is equivalent to the set difference of seq1 and seq2. */
  public static boolean[] setDiff(boolean[] seq1, boolean[] seq2) {
    boolean[] intermediate = new boolean[seq1.length];
    int length = 0;
    for (int i = 0; i < seq1.length; i++) {
      if (!memberOf(seq1[i], seq2)) {
        intermediate[length++] = seq1[i];
      }
    }
    return subarray(intermediate, 0, length);
  }

  public static boolean memberOf(boolean elt, boolean[] arr) {
    for (int i = 0; i < arr.length; i++) {
      if (arr[i] == elt) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("index") // not relevant to this test case
  public static boolean[] subarray(
      boolean[] a, @IndexOrHigh("#1") int startindex, @IndexOrHigh("#1") int length) {
    boolean[] result = new boolean[length];
    System.arraycopy(a, startindex, result, 0, length);
    return result;
  }
}
