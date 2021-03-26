public class ExampleUsage {
  /**
   * this class contains a set of test methods that are supposed to show how the lowerbound checker
   * should work in practice. They contain no annotations - the only test is whether or not it
   * alarms on particular code constructs that are or are not safe
   */
  void safe_loop_const() {
    int[] arr = new int[5];
    int k;
    for (int i = 0; i < 5; i++) {
      k = arr[i];
    }
  }

  void safe_loop_spooky() {
    int[] arr = new int[5];
    int k;
    for (int i = -1; i < 4; ) {
      i++;
      k = arr[i];
    }
  }

  void obviously_unsafe_loop() {
    int[] arr = new int[5];
    int k;
    for (int i = -1; i < 5; i++) {
      // :: error: (array.access.unsafe.low)
      k = arr[i];
    }
  }
}
// a comment
