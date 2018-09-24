import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestGenericUpperBounds {
    // :: error: (invalid.upper.bound.on.type.argument)
    static <T extends @NonDet Object> void foo(@PolyDet List<T> list) {
        //        System.out.println(list.get(0));
    }

    // :: error: (invalid.upper.bound.on.type.argument)
    <T extends @NonDet Object> void callFoo(@OrderNonDet List<T> lst) {
        // :: error: (invalid.upper.bound.on.type.argument)
        foo(lst);
    }

    // :: error: (invalid.upper.bound.on.type.argument)
    void wildCardList(@Det List<?> lst) {}

    // :: error: (invalid.upper.bound.on.type.argument)
    void wildCardListBoundedIncorrect(@Det List<? extends @NonDet Object> lst) {}

    void wildCardListBoundedCorrect(@Det List<? extends @Det Object> lst) {}

    // :: error: (invalid.upper.bound.on.type.argument)
    public static <Z> void copy(List<? extends Z> src) {}

    // :: error: (invalid.upper.bound.on.type.argument)
    public static <Z extends T, T> void copy1(List<? extends Z> src) {}
}
