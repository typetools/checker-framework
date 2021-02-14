// Test case for Issue 388:
// https://github.com/typetools/checker-framework/issues/388

import java.util.Map;

public class Issue388 {
    static class Holder {
        static final String KEY = "key";
    }

    public String getOrDefault(Map<String, String> map, String defaultValue) {
        if (map.containsKey(Holder.KEY)) {
            return map.get(Holder.KEY);
        } else {
            return defaultValue;
        }
    }
}
