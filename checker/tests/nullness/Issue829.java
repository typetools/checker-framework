// Test case for Issue 829
// https://github.com/typetools/checker-framework/issues/829

import org.checkerframework.checker.nullness.qual.*;

public class Issue829 {
  public static @Nullable Double getDouble(boolean flag) {
    return flag ? null : 1.0;
  }

  public static Double getDoubleError(boolean flag) {
    // :: error: (return)
    return flag ? null : 1.0;
  }
}
