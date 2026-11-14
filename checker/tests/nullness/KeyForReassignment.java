// Test case for issue #4105: https://github.com/typetools/checker-framework/issues/4105
// When a local variable gets a new value, or when a new variable with the same name comes into
// scope, @KeyFor annotations that mention the variable's name are no longer valid.

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.KeyFor;

public class KeyForReassignment {
  void reassign(String k) {
    Map<String, Integer> m = new HashMap<>();
    if (m.containsKey(k)) {
      @KeyFor("m") String k1 = k;
      m = new HashMap<>();
      // :: error: (assignment)
      @KeyFor("m") String k2 = k;
    }
  }

  void redeclare(String k) {
    {
      Map<String, Integer> m = new HashMap<>();
      if (!m.containsKey(k)) {
        return;
      }
      @KeyFor("m") String k1 = k;
    }
    {
      Map<String, Integer> m = new HashMap<>();
      // :: error: (assignment)
      @KeyFor("m") String k2 = k;
    }
  }

  void lambdaParam(String k) {
    {
      Map<String, Integer> m = new HashMap<>();
      if (!m.containsKey(k)) {
        return;
      }
    }
    Function<Map<String, Integer>, String> f =
        m -> {
          // :: error: (assignment)
          @KeyFor("m") String k2 = k;
          return k2;
        };
  }

  void lambdaOk(Map<String, Integer> m, String k) {
    if (!m.containsKey(k)) {
      return;
    }
    Function<Map<String, Integer>, String> f =
        other -> {
          @KeyFor("m") String k2 = k;
          return k2;
        };
  }

  void catchParam(String k) {
    {
      Map<String, Integer> m = new HashMap<>();
      if (!m.containsKey(k)) {
        return;
      }
    }
    try {
      k.length();
    } catch (RuntimeException m) {
      // :: error: (assignment)
      @KeyFor("m") String k2 = k;
    }
  }

  void selfReference() {
    Map<@KeyFor("m") String, Integer> m = new HashMap<>();
    Map<@KeyFor("m") String, Integer> m2 = m;
  }

  void pattern(String k, Object o) {
    {
      Map<String, Integer> m = new HashMap<>();
      if (!m.containsKey(k)) {
        return;
      }
    }
    if (o instanceof HashMap<?, ?> m) {
      // :: error: (assignment)
      @KeyFor("m") String k2 = k;
    }
  }
}
