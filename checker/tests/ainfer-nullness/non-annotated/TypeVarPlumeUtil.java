// test case for https://github.com/typetools/checker-framework/issues/5708

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TypeVarPlumeUtil<V extends Object> {
  @SuppressWarnings({
    "nullness" // only check for crashes. Also, was present in the original source file (so the
    // annotations in this code were preserved by RemoveAnnotationsForInference).
  })
  public V merge(@NonNull V value) {
    return value;
  }

  public V mergeNullable(@Nullable V value) {
    // :: warning: (return)
    return value;
  }
}
