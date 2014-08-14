// Test case for Issue 331:
// https://code.google.com/p/checker-framework/issues/detail?id=331

import java.util.List;
import java.util.ArrayList;

class TestTeranry {
    void foo(boolean b, List<Object> res) {
        Object o = b ? "x" : (b ? "y" : "z");
        res.add(o);
    }
}
