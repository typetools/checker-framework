public class Reassignment {
  void test(int[] arr, int i) {
    if (i > 0 && i < arr.length) {
      arr = new int[0];
      // :: error: (array.access.unsafe.high.range)
      int j = arr[i];
    }
  }
}
