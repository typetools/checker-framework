// Test case for Issue 328:
// https://github.com/typetools/checker-framework/issues/328

import java.util.Map;
import org.checkerframework.checker.nullness.qual.*;

public class KeyForIssue328 {
  public static void m(Map<Object, Object> a, Map<Object, Object> b, Object ka, Object kb) {
    if (a.containsKey(ka)) {
      @NonNull Object i = a.get(ka); // OK
    }
    if (b.containsKey(kb)) {
      @NonNull Object i = b.get(kb); // OK
    }
    if (a.containsKey(ka) && b.containsKey(kb)) {
      @NonNull Object i = a.get(ka); // OK
      @NonNull Object j = b.get(kb); // OK
    }
  }
}
