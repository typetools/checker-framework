import org.checkerframework.common.value.qual.*;

public class GTETransferBug {
  void gte_bad_check(int[] a) {
    if (a.length >= 1) {
      // :: error: (assignment)
      int @ArrayLenRange(from = 2) [] b = a;

      int @ArrayLenRange(from = 1) [] c = a;
    }
  }
}
