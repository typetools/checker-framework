// Test case for https://github.com/typetools/checker-framework/issues/2448

import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue2448 {
  static class Option<T> {}

  static <T> Option<T> get(Class<T> klass) {
    throw new UnsupportedOperationException();
  }

  static final Option<@Nullable String> V = get(String.class);
}
