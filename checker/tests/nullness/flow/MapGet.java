// Test case for issue #372:
// https://github.com/typetools/checker-framework/issues/372

// @skip-test until the issue is fixed

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class MapGet {
  private final Map<String, String> labels = new HashMap<>();

  void foo1(String v) {
    labels.put(v, "");
    labels.get(v).toString();
  }

  @NonNull String foo2(String v) {
    labels.put(v, "");
    return labels.get(v);
  }

  @EnsuresNonNull("labels.get(#1)")
  void foo3(String v) {
    labels.put(v, "");
  }
}
