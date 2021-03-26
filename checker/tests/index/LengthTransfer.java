public class LengthTransfer {
  void exceptional_control_flow(int[] a) {
    if (a.length == 0) {
      throw new IllegalArgumentException();
    }
    int i = a[0];
  }

  void equal_to_return(int[] a) {
    if (a.length == 0) {
      return;
    }
    int i = a[0];
  }

  void gt_check(int[] a) {
    if (a.length > 0) {
      int i = a[0];
    }
  }
}
