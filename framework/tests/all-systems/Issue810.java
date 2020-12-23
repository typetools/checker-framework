// Test case for Issue 810
// https://github.com/typetools/checker-framework/issues/810

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Issue810 {
    Map<String, String> m = new HashMap<>();
    Set<String> n = m.keySet();
}
