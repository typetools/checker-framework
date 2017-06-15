
import org.checkerframework.checker.index.qual.*;

class FieldInvalidation {
    final FieldInvalidation x;
    FieldInvalidation y;
    public int z;

    private FieldInvalidation() {
        x = null;
    }

    void test1(int[] a, @LTLengthOf(value = "#1", offset = "x.z") int i) {}

    void test2(int[] a, @LTLengthOf(value = "#1", offset = "y.z") int i) {
        //:: error: (reassignment.field.not.permitted)
        y = new FieldInvalidation();
    }
}
