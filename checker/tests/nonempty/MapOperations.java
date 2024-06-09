import java.util.Map;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

class MapOperations {

  // Skip test until we decide whether to handle accesses on empty containers
  // void addToMapParam(Map<String, Integer> m) {
  //   // :: error: (method.invocation)
  //   m.get("hello");

  //   m.put("hello", 1);

  //   @NonEmpty Map<String, Integer> m2 = m; // OK
  //   m.get("hello"); // OK
  // }

  // Skip test until we decide whether to handle accesses on empty containers
  // void clearMap(Map<String, Integer> m) {
  //   m.put("hello", 1);
  //   m.get("hello"); // OK

  //   m.clear();
  //   // :: error: (method.invocation)
  //   m.get("hello");
  // }

  void containsKeyRefinement(Map<String, Integer> m, String key) {
    if (m.containsKey(key)) {
      @NonEmpty Map<String, Integer> m2 = m; // OK
    } else {
      // :: error: (assignment)
      @NonEmpty Map<String, Integer> m2 = m; // OK
    }
  }

  void containsValueRefinement(Map<String, Integer> m, Integer value) {
    if (m.containsValue(value)) {
      @NonEmpty Map<String, Integer> m2 = m;
    } else {
      // :: error: (assignment)
      @NonEmpty Map<String, Integer> m2 = m;
    }
  }
}
