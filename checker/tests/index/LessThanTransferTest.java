import org.checkerframework.common.value.qual.MinLen;

public class LessThanTransferTest {
  void lt_check(int[] a) {
    if (0 < a.length) {
      int @MinLen(1) [] b = a;
    }
  }

  void lt_bad_check(int[] a) {
    if (0 < a.length) {
      // :: error: (assignment)
      int @MinLen(2) [] b = a;
    }
  }
}
