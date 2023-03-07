// test case for https://github.com/typetools/checker-framework/issues/5708

import org.checkerframework.checker.nullness.qual.NonNull;

@SuppressWarnings({
  "nullness" // only check for crashes. Also, was present in the original source file (so the
  // annotations in this code were preserved by RemoveAnnotationsForInference).
})
public class TypeVarPlumeUtil<V extends Object> {
  public V merge(@NonNull V value) {
    return value;
  }
}
