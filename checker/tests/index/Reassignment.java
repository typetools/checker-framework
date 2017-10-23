import org.checkerframework.checker.index.qual.*;
import org.checkerframework.dataflow.qual.Pure;

class Reassignment {

    private int[] b;
    private int[] d;

    @IndexFor("b") int bi;

    @Pure
    int[] id(int[] a) {
        return a;
    }

    void test(int[] arr, int i, @IndexFor("#1") int k) {
        if (i > 0 && i < arr.length) {
            //:: error: (reassignment.not.permitted)
            arr = new int[0];
            //:: error: (array.access.unsafe.high.range)
            int j = arr[i];
            d = new int[0];
        }
    }

    void methodCallTest(@NonNegative int x) {
        if (x < id(b).length) {
            //:: error: (reassignment.not.permitted)
            b = new int[0];
            //:: error: (array.access.unsafe.high.range)
            int y = id(b)[x];
        }
        if (b.length > 0) {
            //:: error: (side.effect.invalidation)
            test(b, 0, b.length - 1);
        }
    }

    public void method(@IndexFor("#2") int a, String[] array) {}

    public void method2(int a, String[] array) {
        array = new String[] {""};
    }
}
