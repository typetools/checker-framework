import org.checkerframework.checker.index.qual.*;
import org.checkerframework.dataflow.qual.Pure;

class Reassignment {

    private int[] b;
    private int[] c;

    @IndexFor("b") int bi;

    @Pure
    int[] id(int[] a) {
        return a;
    }

    void test0(@IndexFor("id(c)") int i) {
        //:: error: (reassignment.not.permitted)
        c = new int[0];
    }

    void test(int[] arr, int i, @IndexFor("b") int k) {
        if (i > 0 && i < arr.length) {
            arr = new int[0];
            //:: error: (array.access.unsafe.high)
            int j = arr[i];
            //:: error: (reassignment.not.permitted)
            b = new int[0];
        }
    }

    void methodCallTest(@NonNegative int x) {
        if (x < id(b).length) {
            //:: error: (reassignment.not.permitted)
            b = new int[0];
            //:: error: (array.access.unsafe.high)
            @IndexFor("id(b)")
            int y = x;
        }
        if (b.length > 0) {
            test(b, 0, b.length - 1);
        }
    }
}
