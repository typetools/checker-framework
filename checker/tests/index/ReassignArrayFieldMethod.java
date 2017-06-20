import org.checkerframework.checker.index.qual.*;
import org.checkerframework.dataflow.qual.Pure;

class ReassignArrayFieldMethod {

    private int[] b;
    private int[] c;

    @Pure
    int[] id(int[] a) {
        return a;
    }

    void test(@IndexFor("id(c)") int x) {
        //:: error: (reassignment.field.not.permitted.method)
        c = new int[0];
    }

    void test1(@NonNegative int x) {
        if (x < id(b).length) {
            int j = this.id(b)[x];
            // Not legal, because there is a written type with a method in it,
            // which this *might* invalidate (it doesn't in this case).
            //:: error: (reassignment.field.not.permitted.method)
            b = new int[0];
            //:: error: (array.access.unsafe.high.range)
            int k = this.id(b)[x];
        }
    }
}
