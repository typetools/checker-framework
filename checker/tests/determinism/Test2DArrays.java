import org.checkerframework.checker.determinism.qual.*;

public class Test2DArrays<T> {
    void testAccess(@Det int @NonDet [] @Det [] arr) {
        // :: error: (assignment.type.incompatible)
        @Det int x = arr[0][0];
    }
}
