import org.checkerframework.common.value.qual.*;

public class MinLenEqTransfer {
  void eq_check(int[] a) {
    if (1 == a.length) {
      int @MinLen(1) [] b = a;
    }
    if (a.length == 1) {
      int @MinLen(1) [] b = a;
    }
  }

  void eq_bad_check(int[] a) {
    if (1 == a.length) {
      // :: error: (assignment.type.incompatible)
      int @MinLen(2) [] b = a;
    }
  }

  int @MinLen(2) [] test(int[] a) {
    if (a.length == 100 || a.length == 3) {
      int x = a.length;
      return a;
    } else if (a.length == 0 || a.length == 1) {
      int x = a.length;
      // :: error: (return.type.incompatible)
      return a;
    }
    return new int[] {1, 2};
  }
}
