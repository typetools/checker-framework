import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

public class Issue20 {
  // An issue with LUB that results in losing information when unifying.
  int[] a, b;

  void test(@LTLengthOf("a") int i, @LTEqLengthOf({"a", "b"}) int j, boolean flag) {
    @LTEqLengthOf("a") int k = flag ? i : j;
  }
}
