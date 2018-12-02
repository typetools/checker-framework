// Test for Checker Framework issue 795
// https://github.com/typetools/checker-framework/issues/795

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.lock.qual.*;

class GuardedByLocalVariable {

    public static void localVariableShadowing() {
        // :: error: (expression.unparsable.type.invalid)
        @GuardedBy("m0") Object kk;
        {
            final Map<Object, Integer> m0 = new HashMap<>();
            @GuardedBy("m0") Object k = "key";
            // :: error: (assignment.type.incompatible)
            kk = k;
        }
        {
            final Map<Object, Integer> m0 = new HashMap<>();
            // :: error: (assignment.type.incompatible)
            @GuardedBy("m0") Object k2 = kk;
        }
    }

    public static void invalidLocalVariable() {
        // :: error: (expression.unparsable.type.invalid)
        @GuardedBy("foobar") Object kk;
    }
}
