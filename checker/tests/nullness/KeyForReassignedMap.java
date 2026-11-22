// Test case for https://github.com/typetools/checker-framework/issues/8621 .
// Reassigning a map must not leave a `@KeyFor` type that names it unchanged.

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;

public class KeyForReassignedMap {

  Map<String, Object> f = new HashMap<>();

  // The declared type of the parameter `k` names the parameter `m`.
  void declaredParameterNamesParameter(
      @KeyFor("#2") String k, Map<String, Object> m, Map<String, Object> other) {
    // :: error: [reassignment.invalidates.type]
    m = other;
    m.get(k).toString();
  }

  // The declared type of the parameter `k` names the field `f`.
  void declaredParameterNamesField(@KeyFor("f") String k, Map<String, Object> other) {
    // :: error: [reassignment.invalidates.type]
    f = other;
    f.get(k).toString();
  }

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

  // No declared type names `other`.
  void declaredTypeNamesOtherMap(
      @KeyFor("#2") String k, Map<String, Object> m, Map<String, Object> other) {
    other = new HashMap<>();
    m.get(k).toString();
  }

  // A self-assignment does not change the value.
  void selfAssignment(@KeyFor("#2") String k, Map<String, Object> m) {
    m = m;
    m.get(k).toString();
  }

  // `k` is a key for `other`, so it is still a key for `m` after `m = other`.
  void keyForNewValue(
      @KeyFor({"#2", "#3"}) String k, Map<String, Object> m, Map<String, Object> other) {
    m = other;
    m.get(k).toString();
  }

  // A refined type that names a different map survives the reassignment.
  void refinedForOtherMap(String k, Map<String, Object> m, Map<String, Object> other) {
    if (m.containsKey(k)) {
      other = new HashMap<>();
      m.get(k).toString();
    }
  }
}
