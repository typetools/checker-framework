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
}
