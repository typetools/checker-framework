import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

public class TestLocalArrays<T> {
    void testarr(@NonDet int @NonDet [] a) {
        // :: error: (assignment.type.incompatible)
        int b[] = a;
    }

    void testTypeParameters(@NonDet ArrayList<@NonDet Integer> a) {
        // :: error: (assignment.type.incompatible)
        ArrayList<Integer> b = a;
    }

    <@Det T> void issue392(@Det T t) {
        Object o = new @Det Object @Det [] {t, t};
    }
}
