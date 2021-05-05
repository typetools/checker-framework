// Test case for https://github.com/typetools/checker-framework/issues/4593 .

// @skip-test until the bug is fixed

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue4593 {

  void getContext(@Nullable String nble) {
    Map<String, @Nullable Object> map = new HashMap<>();
    map.put("configDir", nble);
  }

  void getContextWithVar(@Nullable String nble) {
    var map = new HashMap<String, @Nullable Object>();
    map.put("configDir", nble);
  }
}
