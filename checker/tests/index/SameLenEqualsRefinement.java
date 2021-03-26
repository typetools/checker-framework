import org.checkerframework.checker.index.qual.*;
import org.checkerframework.dataflow.qual.Pure;

public class SameLenEqualsRefinement {
  void transfer3(int @SameLen("#2") [] a, int[] b, int[] c) {
    if (a == c) {
      for (int i = 0; i < c.length; i++) { // i's type is @LTL("c")
        b[i] = 1;
        int @SameLen({"a", "b", "c"}) [] d = c;
      }
    }
  }

  void transfer4(int[] a, int[] b, int[] c) {
    if (b == c) {
      if (a == b) {
        for (int i = 0; i < c.length; i++) { // i's type is @LTL("c")
          a[i] = 1;
          int @SameLen({"a", "b", "c"}) [] d = c;
        }
      }
    }
  }

  void transfer5(int[] a, int[] b, int[] c, int[] d) {
    if (a == b && b == c) {
      int[] x = a;
      int[] y = x;
      int index = x.length - 1;
      if (index > 0) {
        f(a[index]);
        f(b[index]);
        f(c[index]);
        f(x[index]);
        f(y[index]);
      }
    }
  }

  @Pure
  void f(Object o) {}
}
