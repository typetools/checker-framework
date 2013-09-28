// In source code, whether an explicit upper bound affects what type
// qualifier is imputed for that upper bound.
// The JDK should be treated consistently with source code.

// @skip-test Commented out to avoid breaking the build

import java.util.Collections;
import java.util.Map;

public class SourceVsJdk<K, V> {
    public Map<K, V> getMap() {
        return Collections.<K, V> emptyMap();
    }
}
