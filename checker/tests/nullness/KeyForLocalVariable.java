// Test for Checker Framework issue 795
// https://github.com/typetools/checker-framework/issues/795

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.*;

public class KeyForLocalVariable {

    public static void localVariableShadowing() {
        // :: error: (expression.unparsable.type.invalid)
        @KeyFor("m0") String kk;
        {
            Map<String, Integer> m0 = new HashMap<>();
            @SuppressWarnings("keyfor")
            @KeyFor("m0") String k = "key";
            // :: error: (assignment.type.incompatible)
            kk = k;
        }
        {
            Map<String, Integer> m0 = new HashMap<>();
            // :: error: (assignment.type.incompatible)
            @KeyFor("m0") String k2 = kk;
        }
    }

    public static void invalidLocalVariable() {
        // :: error: (expression.unparsable.type.invalid)
        @KeyFor("foobar") String kk;
    }
}
