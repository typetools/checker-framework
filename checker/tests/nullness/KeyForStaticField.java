// Test case for Issue 877:
// https://github.com/typetools/checker-framework/issues/877
// @skip-test until the issue is fixed.

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.*;

class KeyForStaticField {
    @SuppressWarnings("keyfor")
    public static final @KeyFor("this.map") String STATIC_KEY = "some text";

    private Map<String, Integer> map;

    public KeyForStaticField() {
        map = new HashMap<>();
        map.put(STATIC_KEY, 0);
    }

    /** Returns the value for the given key, which must be present in the map. */
    public Integer getValue(@KeyFor("this.map") String key) {
        assert map.containsKey(key) : "Map does not contain key " + key;
        return map.get(key);
    }

    public void m(KeyForStaticField other) {
        getValue(STATIC_KEY);
        this.getValue(STATIC_KEY);
        other.getValue(STATIC_KEY);
    }
}
