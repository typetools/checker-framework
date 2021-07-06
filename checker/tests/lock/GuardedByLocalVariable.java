// Test for Checker Framework issue 795
// https://github.com/typetools/checker-framework/issues/795

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.lock.qual.*;

public class GuardedByLocalVariable {

  public static void localVariableShadowing() {
    // :: error: (expression.unparsable)
    @GuardedBy("m0") Object kk;
    {
      @SuppressWarnings("assignment") // prevent flow-sensitive type refinement
      final Map<Object, Integer> m0 = someValue();
      @GuardedBy("m0") Object k = "key";
      // If the type of kk were legal, this assignment would be illegal because the two instances of
      // "m0" would refer to different variables.
      kk = k;
    }
    {
      @SuppressWarnings("assignment") // prevent flow-sensitive type refinement
      final Map<Object, Integer> m0 = someValue();
      // If the type of kk were legal, this assignment would be illegal because the two instances of
      // "m0" would refer to different variables.
      @GuardedBy("m0") Object k2 = kk;
    }
  }

  public static void invalidLocalVariable() {
    // :: error: (expression.unparsable)
    @GuardedBy("foobar") Object kk;
  }

  static @GuardedByUnknown Map<Object, Integer> someValue() {
    return new HashMap<>();
  }
}
