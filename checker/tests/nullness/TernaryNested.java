// Test case for Issue 331:
// https://code.google.com/p/checker-framework/issues/detail?id=331
//@skip-test
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.initialization.qual.*;

import java.util.List;

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
