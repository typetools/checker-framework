// Test case for Issue 849:
// https://github.com/typetools/checker-framework/issues/849

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue849 {
  class Gen<T> {}

  void nullness(Gen<Gen<@NonNull Object>> genGenNonNull) {
    // :: error: (assignment.type.incompatible)
    Gen<@Nullable ? extends @Nullable Gen<@Nullable Object>> a = genGenNonNull;
  }
}
