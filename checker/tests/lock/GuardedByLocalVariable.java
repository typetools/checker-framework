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
      final Map<Object, Integer> m0 = new HashMap<>();
      @GuardedBy("m0") Object k = "key";
      // :: error: (assignment)
      kk = k;
    }
    {
      final Map<Object, Integer> m0 = new HashMap<>();
      // :: error: (assignment)
      @GuardedBy("m0") Object k2 = kk;
    }
  }

  public static void invalidLocalVariable() {
    // :: error: (expression.unparsable)
    @GuardedBy("foobar") Object kk;
  }
}
