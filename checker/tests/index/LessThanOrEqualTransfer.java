import org.checkerframework.common.value.qual.MinLen;

public class LessThanOrEqualTransfer {
  void lte_check(int[] a) {
    if (1 <= a.length) {
      int @MinLen(1) [] b = a;
    }
  }

  void lte_bad_check(int[] a) {
    if (1 <= a.length) {
      // :: error: (assignment)
      int @MinLen(2) [] b = a;
    }
  }
}
