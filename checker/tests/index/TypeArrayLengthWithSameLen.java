import org.checkerframework.checker.index.qual.*;

public class TypeArrayLengthWithSameLen {
  void test(int @SameLen("#2") [] a, int @SameLen("#1") [] b, int[] c) {
    if (a.length == c.length) {
      @LTEqLengthOf({"a", "b", "c"}) int x = b.length;
    }
  }
}
