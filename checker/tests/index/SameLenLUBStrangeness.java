import org.checkerframework.checker.index.qual.SameLen;

public class SameLenLUBStrangeness {
  void test(int[] a, boolean cond) {
    int[] b;
    if (cond) {
      b = a;
    }
    // :: error: (assignment)
    int @SameLen({"a", "b"}) [] c = a;
  }
}
