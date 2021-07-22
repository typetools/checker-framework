// Tests that converting to a String from an object doesn't create
// phantom must call obligations. Taken from some code in Zookeeper that
// caused a false positive.

import java.util.Map;
import java.util.Map.Entry;

class StringFromObject {
    boolean test(Map<Object, Object> map) {
        boolean isHierarchical = false;
        for (Entry<Object, Object> entry : map.entrySet()) {
            String key = entry.getKey().toString().trim();
            if (key.startsWith("group") || key.startsWith("weight")) {
                isHierarchical = true;
                break;
            }
        }
        return isHierarchical;
    }
}
