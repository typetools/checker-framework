import org.checkerframework.checker.nullness.qual.*;

import java.util.List;

public class WildcardSubtypingTypeArray<A extends @Nullable Object> {
    void test(List<? extends A> list) {
        test2(list.get(0));
    }

    void test2(A x) {}
}
