// test case for https://github.com/typetools/checker-framework/issues/5708

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TypeVarPlumeUtil<V extends Object> {
  public V merge(@NonNull V value) {
    return value;
  }

  public V mergeNullable(@Nullable V value) {
    // :: warning: [return]
    return value;
  }
}
