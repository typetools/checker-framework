// Test case for Issue 328:
// https://code.google.com/p/checker-framework/issues/detail?id=328
// @skip-tests

import java.util.Map;

import org.checkerframework.checker.nullness.qual.*;

class Test {
  public static void m(Map<Object, Object> a, Map<Object, Object> b, Object ka, Object kb) {
    if (a.containsKey(ka)) {
      @NonNull Object i = a.get(ka); // OK
    }
    if (b.containsKey(kb)) {
      @NonNull Object i = b.get(kb); // OK
    }
    if (a.containsKey(ka) && b.containsKey(kb)) {
      @NonNull Object i = a.get(ka); // ERROR, but should work
      @NonNull Object j = b.get(kb); // ERROR, but should work
    }
  }
}
