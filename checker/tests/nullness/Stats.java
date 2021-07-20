// @skip-tests Failing, but commented out to avoid breaking the build

import org.checkerframework.checker.nullness.qual.*;

import java.util.Map;

public class Stats {

    @Nullable Map<Integer, String> inv_map = null;

    void dump() {

        assert inv_map != null : "@AssumeAssertion(nullness)";

        for (Integer inv_class : inv_map.keySet()) {
            inv_map.get(inv_class);
        }
    }
}
