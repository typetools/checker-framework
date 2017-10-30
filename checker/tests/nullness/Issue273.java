// Test case for issue #273:
// https://github.com/typetools/checker-framework/issues/273

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.*;

class Issue273 {
    public static void main(String... p) {
        Map<String, Integer> m0 = new HashMap<String, Integer>();
        Map<String, Integer> m1 = new HashMap<String, Integer>();
        @SuppressWarnings("assignment.type.incompatible")
        @KeyFor("m0") String k = "key";
        m0.put(k, 1);

        // :: error: (argument.type.incompatible)
        getMap2(m0, m1, k).toString();
    }

    public static @NonNull Integer getMap2(
            Map<String, Integer> m1, // m1,m0 flipped
            Map<String, Integer> m0,
            @KeyFor("#2") String k) {
        return m0.get(k);
    }
}
