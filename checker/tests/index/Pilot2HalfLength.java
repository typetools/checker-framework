// test case for issue 158: https://github.com/kelloggm/checker-framework/issues/158

// @skip-test until fixed

public class Pilot2HalfLength {
  private static int[] getFirstHalf(int[] array) {
    int[] firstHalf = new int[array.length / 2];
    for (int i = 0; i < firstHalf.length; i++) {
      firstHalf[i] = array[i];
    }
    return firstHalf;
  }
}
