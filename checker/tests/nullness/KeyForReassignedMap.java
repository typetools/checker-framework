// Test case for https://github.com/typetools/checker-framework/issues/8621 .
// Reassigning a map does not invalidate a `@KeyFor` type that names it.

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;

public class KeyForReassignedMap {

  Map<String, Object> f = new HashMap<>();

  // The declared type of the parameter `k` names the parameter `m`.
  void declaredParameterNamesParameter(
      @KeyFor("#2") String k, Map<String, Object> m, Map<String, Object> other) {
    m = other;
    // :: error: [dereference.of.nullable]
    m.get(k).toString();
  }

  // The declared type of the parameter `k` names the field `f`.
  void declaredParameterNamesField(@KeyFor("f") String k, Map<String, Object> other) {
    f = other;
    // :: error: [dereference.of.nullable]
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
}
