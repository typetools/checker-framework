import org.checkerframework.common.value.qual.*;

public class MinLenGTETransfer {
  void gte_check(int[] a) {
    if (a.length >= 1) {
      int @MinLen(1) [] b = a;
    }
  }

  void gte_bad_check(int[] a) {
    if (a.length >= 1) {
      // :: error: (assignment)
      int @MinLen(2) [] b = a;
    }
  }
}
