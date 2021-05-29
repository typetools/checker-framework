// Test case for Issue 339:
// https://github.com/typetools/checker-framework/issues/339

import org.checkerframework.checker.nullness.qual.*;

public class Issue339<S> {
  static <T> @NonNull T checkNotNull(T p) {
    throw new RuntimeException();
  }

  void m(@Nullable S s) {
    @NonNull S r1 = Issue339.<@Nullable S>checkNotNull(s);
    @NonNull S r2 = Issue339.checkNotNull(s);
    @NonNull S r3 = Issue339.checkNotNull(null);
  }
}
