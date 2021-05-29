// Issue #594: https://github.com/typetools/checker-framework/issues/594
// The bug is that the error message has identical found and required:
//
//                 return result;
//                        ^
//   found   : T extends @Initialized @Nullable Object
//   required: T extends @Initialized @Nullable Object

import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue594<T> {
  private @Nullable T result = null;

  // Should return @Nullable T
  private T getResult() {
    // :: error: (return)
    return result;
  }
}
