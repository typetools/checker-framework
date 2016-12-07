// Test for Checker Framework issue 795
// https://github.com/typetools/checker-framework/issues/795

// @skip-test until the issue is fixed

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.lock.qual.*;

class GuardedByLocalVariable {

    public static void localVariableShadowing() {
        //:: error: (flowexpr.parse.error)
        @GuardedBy("m0") Object kk;
        {
            final Map<Object, Integer> m0 = new HashMap<Object, Integer>();
            @GuardedBy("m0") Object k = "key";
            kk = k;
        }
        {
            final Map<Object, Integer> m0 = new HashMap<Object, Integer>();
            //:: error: (assignment.type.incompatible)
            @GuardedBy("m0") Object k2 = kk;
        }
    }

    public static void invalidLocalVariable() {
        //:: error: (flowexpr.parse.error)
        @GuardedBy("foobar") Object kk;
    }
}
