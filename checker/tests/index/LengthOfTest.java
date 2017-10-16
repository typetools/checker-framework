
import org.checkerframework.checker.index.qual.*;

class LengthOfTest {
    //@ SuppressWarnings("local.variable.unsafe.dependent.annotation")
    void foo(int[] a, @LengthOf("#1") int x) {
        @IndexOrHigh("a") int y = x;
        //:: error: (assignment.type.incompatible)
        @IndexFor("a") int w = x;
        @LengthOf("a") int z = a.length;
    }
}
