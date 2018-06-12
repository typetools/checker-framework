import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

public class TestLocalArrays<T> {
    void testarr(@NonDet int @NonDet [] a) {
        // :: error: (assignment.type.incompatible)
        @Det int @Det [] b = a;
    }

    void testTypeParameters(@NonDet ArrayList<@NonDet Integer> a) {
        // :: error: (assignment.type.incompatible)
        @Det ArrayList<@Det Integer> b = a;
    }

    <@Det T> void issue392(@Det T t) {
        Object o = new @Det Object @Det [] {t, t};
    }
}
