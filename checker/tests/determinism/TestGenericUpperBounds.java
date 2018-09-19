import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestGenericUpperBounds {
    static <T extends @NonDet Object> void foo(@PolyDet List<T> list) {
        //        System.out.println(list.get(0));
    }

    <T extends @NonDet Object> void callFoo(@OrderNonDet List<T> lst) {
        foo(lst);
    }
}
