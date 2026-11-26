// Test case for https://github.com/typetools/checker-framework/issues/8621 .
// Reassigning a map must not leave a `@KeyFor` type that names it unchanged.

import java.util.HashMap;
import java.util.Map;

public class KeyForReassignedMap {

  Map<String, Object> f = new HashMap<>();

  // The refined type of `k` names `m`.
  void refined(String k, Map<String, Object> m, Map<String, Object> other) {
    if (m.containsKey(k)) {
      m = other;
      // :: error: [dereference.of.nullable]
      m.get(k).toString();
    }
  }

  //
  // Reassignments that must be accepted
  //

  // A refined type that names a different map survives the reassignment.
  void refinedForOtherMap(String k, Map<String, Object> m, Map<String, Object> other) {
    if (m.containsKey(k)) {
      other = new HashMap<>();
      m.get(k).toString();
    }
  }
}
