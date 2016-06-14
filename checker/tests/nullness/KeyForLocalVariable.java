// Test for Checker Framework issue 795
// https://github.com/typetools/checker-framework/issues/795

// @skip-test until the issue is fixed

import org.checkerframework.checker.nullness.qual.*;
import java.util.Map;
import java.util.HashMap;

class KeyForLocalVariable {

    public static void localVariableShadowing() {
        // There should be a warning here that m0 is not in scope
        @KeyFor("m0") String kk;
        {
            Map<String, Integer> m0 = new HashMap<String,Integer>();
            @SuppressWarnings("keyfor")
            @KeyFor("m0") String k = "key";
            kk = k;
        }
        {
            Map<String, Integer> m0 = new HashMap<String,Integer>();
            //:: error: (assignment.type.incompatible)
            @KeyFor("m0") String k2 = kk;
        }
    }

    public static void invalidLocalVariable() {
        // There should be a warning here that m0 is not in scope
        @KeyFor("foobar") String kk;
    }

}
