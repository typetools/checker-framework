// Test case for Issue 331:
// https://github.com/typetools/checker-framework/issues/331
// @skip-test
import java.util.List;
import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class TernaryNested {
    Object foo(boolean b) {
        Object o = b ? "" : (b ? "" : "");
        return o;
    }

    void bar(List<Object> l, boolean b) {
        Object o = b ? "" : (b ? "" : "");
        l.add(o);
    }
}
