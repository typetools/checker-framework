import org.checkerframework.checker.index.qual.SameLen;

public class ArrayLengthEquality {
  void test(int[] a, int[] b) {
    if (a.length == b.length) {
      int @SameLen({"a", "b"}) [] c = a;
      int @SameLen({"a", "b"}) [] d = b;
    }
    if (a.length != b.length) {
      // Do nothing.
      int x = 0;
    } else {
      int @SameLen({"a", "b"}) [] e = a;
      int @SameLen({"a", "b"}) [] f = b;
    }
  }
}
