import org.checkerframework.checker.index.qual.*;
import org.checkerframework.dataflow.qual.Pure;

class Reassignment {

    private int[] b;
    private int[] c;
    private int[] d;

    @IndexFor("b") int bi;

    @Pure
    int[] id(int[] a) {
        return a;
    }

    void test0(@IndexFor("id(c)") int i) {
        //:: error: (reassignment.field.not.permitted.method)
        c = new int[0];
    }

    void test(int[] arr, int i, @IndexFor("#1") int k) {
        if (i > 0 && i < arr.length) {
            arr = new int[0];
            //:: error: (array.access.unsafe.high)
            int j = arr[i];
            //:: error: (reassignment.field.not.permitted.method)
            d = new int[0];
        }
    }

    void methodCallTest(@NonNegative int x) {
        if (x < id(b).length) {
            //:: error: (reassignment.field.not.permitted.method)
            b = new int[0];
            //:: error: (array.access.unsafe.high)
            int y = id(b)[x];
        }
        if (b.length > 0) {
            //:: error: (side.effect.invalidation)
            test(b, 0, b.length - 1);
        }
    }
}
