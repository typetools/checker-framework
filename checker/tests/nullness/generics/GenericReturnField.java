// Related to issue #594
//   https://github.com/typetools/checker-framework/issues/594
// but does not reproduce the problem because that issue depends on
// the error message.  See ../../nullness-extra/issue594/

import org.checkerframework.checker.nullness.qual.Nullable;

public class GenericReturnField<T> {
  private @Nullable T result = null;

  // Should return @Nullable T
  private T getResult() {
    // :: error: (return.type.incompatible)
    return result;
  }
}
