// Test case for Issue 438:
// https://github.com/typetools/checker-framework/issues/438

import java.util.HashSet;
import java.util.List;

public class Issue438 {
  boolean foo(List<String> list) {
    if (list.isEmpty()) {
      return new HashSet<>(list).isEmpty();
    } else {
      return new HashSet<>(list).contains("test");
    }
  }

  int bar(List<String> list) {
    return new HashSet<>(list).size();
  }
}
