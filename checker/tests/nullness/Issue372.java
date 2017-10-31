// Test case for Issue 372:
// https://github.com/typetools/checker-framework/issues/372

// @skip-test

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.KeyFor;

class Test {
    private final Map<String, String> labels = new HashMap<>();

    @EnsuresNonNull("labels.get(#1)")
    void foo(@KeyFor("labels") String v) {
        labels.put(v, "");
    }
}
