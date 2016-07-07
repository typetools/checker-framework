// Test case for Issue 810
// https://github.com/typetools/checker-framework/issues/810
// @skip-test

import java.util.*;

class Issue810 {
    Map<String, String> m = new HashMap<>();
    Set<String> n = m.keySet();
}
