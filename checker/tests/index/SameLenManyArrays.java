import org.checkerframework.checker.index.qual.*;
import org.checkerframework.dataflow.qual.Pure;

public class SameLenManyArrays {
    void transfer1(int @SameLen("#2") [] a, int[] b) {
        int[] c = new int[a.length];
        for (int i = 0; i < c.length; i++) { // i's type is @LTL("c")
            b[i] = 1;
            int @SameLen({"a", "b", "c"}) [] d = c;
        }
    }

    void transfer2(int @SameLen("#2") [] a, int[] b) {
        for (int i = 0; i < b.length; i++) { // i's type is @LTL("b")
            a[i] = 1;
        }
    }

    void transfer3(int @SameLen("#2") [] a, int[] b, int[] c) {
        if (a.length == c.length) {
            for (int i = 0; i < c.length; i++) { // i's type is @LTL("c")
                b[i] = 1;
                int @SameLen({"a", "b", "c"}) [] d = c;
            }
        }
    }

    void transfer4(int[] a, int[] b, int[] c) {
        if (b.length == c.length) {
            if (a.length == b.length) {
                for (int i = 0; i < c.length; i++) { // i's type is @LTL("c")
                    a[i] = 1;
                    int @SameLen({"a", "b", "c"}) [] d = c;
                }
            }
        }
    }

    void transfer5(int[] a, int[] b, int[] c, int[] d) {
        if (a.length == b.length && b.length == c.length) {
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
