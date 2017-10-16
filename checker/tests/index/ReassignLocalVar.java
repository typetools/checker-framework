import org.checkerframework.checker.index.qual.*;

//@ SuppressWarnings("local.variable.unsafe.dependent.annotation")
class ReassignLocalVar {

    void test0(int x, int[] a) {
        if (x < a.length) {
            @LTLengthOf("a") int y = x;
            a = new int[0];
            //:: error: (assignment.type.incompatible)
            @LTLengthOf("a") int z = x;
        }
    }

    void test1(int x, int z, int[] b, int[] a) {
        if (x < b.length && z < a.length) {
            b = new int[0];
            @LTLengthOf("a") int y = z;
            //:: error: (assignment.type.incompatible)
            @LTLengthOf("b") int w = x;
        }
    }

    void test3(int[] c, @IndexFor("#1") int x) {
        //:: error: (reassignment.not.permitted)
        c = new int[0];
    }
}
