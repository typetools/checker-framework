// Test case for issue 42: https://github.com/kelloggm/checker-framework/issues/42

// @skip-test until bug is fixed

public class Pilot4Subtraction {

  private static int[] getSecondHalf(int[] array) {
    int len = array.length / 2;
    int b = len - 1;
    int[] arr = new int[len];
    for (int a = 0; a < len; a++) {
      arr[a] = array[b];
      b--;
    }
    return arr;
  }
}
