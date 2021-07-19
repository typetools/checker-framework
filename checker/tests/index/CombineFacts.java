import org.checkerframework.checker.index.qual.LTLengthOf;

@SuppressWarnings("lowerbound")
public class CombineFacts {
  void test(int[] a1) {
    @LTLengthOf("a1") int len = a1.length - 1;
    int[] a2 = new int[len];
    a2[len - 1] = 1;
    a1[len] = 1;

    // This access should issue an error.
    // :: error: (array.access.unsafe.high)
    a2[len] = 1;
  }
}
