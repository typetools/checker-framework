import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestSetEquals {
    void test(@OrderNonDet Set<@Det Integer> set, @OrderNonDet List<@Det Integer> lst) {
        @NonDet Object obj = (@NonDet Object) lst;
        set.equals(obj);
    }
}
