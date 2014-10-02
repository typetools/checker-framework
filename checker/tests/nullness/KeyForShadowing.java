// @skip-test
// Test for Checker Framework issue 273:
// http://code.google.com/p/checker-framework/issues/detail?id=273

import org.checkerframework.checker.nullness.qual.*;
import java.util.Map;
import java.util.HashMap;

class KeyForShadowing {
    public static void main(String... p) {
        Map<String, Integer> m0 = new HashMap<String,Integer>();
        Map<String, Integer> m1 = new HashMap<String,Integer>();
        @SuppressWarnings("assignment.type.incompatible")
        @KeyFor("m0") String k = "key";
        m0.put(k, 1);

        getMap2(m0,m1,k).toString();
    }

    public static @NonNull Integer getMap2(
            Map<String, Integer> m1,  // m1,m0 flipped
            Map<String, Integer> m0,
            @KeyFor("m0") String k)
    {
        return m0.get(k);
    }
}
