// Test case for Issue 979:
// https://github.com/typetools/checker-framework/issues/979

import java.util.ArrayList;
import java.util.List;

public class OneOf {
  static List<String> alist = new ArrayList<>();

  static <V> V oneof(V v1, V v2) {
    return v1;
  }

  @SuppressWarnings("nullness") // don't bother with implementations
  static <T> List<T> empty() {
    return null;
  }
}

class OneOfUse {
  List<String> foo() {
    return OneOf.oneof(OneOf.alist, OneOf.empty());
  }
}
