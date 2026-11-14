// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class InstanceOfRecordPatternTypeArgument {

  static class Box<T extends @NonNull Object> {}

  static class Sub<T extends @NonNull Object> extends Box<T> {}

  record R(String a, Box<String> c) {}

  void recordPattern(Object o) {
    // :: error: (type.argument)
    if (o instanceof R(String a, Sub<@Nullable String> c)) {}
  }

  record Outer(R r) {}

  void nestedRecordPattern(Object o) {
    // :: error: (type.argument)
    if (o instanceof Outer(R(var a, Sub<@Nullable String> c))) {}
  }

  void recordPatternValid(Object o) {
    if (o instanceof R(String a, Sub<String> c)) {}
  }
}
