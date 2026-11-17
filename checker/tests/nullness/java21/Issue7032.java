// Test case for https://github.com/typetools/checker-framework/issues/7032

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue7032 {
  sealed interface IntOrBool {}

  public record WrappedInt(Integer a) implements IntOrBool {}

  public record WrappedBoolean(@Nullable Boolean b) implements IntOrBool {}

  public static void test(IntOrBool i) {
    switch (i) {
      case WrappedInt(Integer x) -> {}
      case WrappedBoolean(Boolean y) -> {
        // :: error: (dereference.of.nullable)
        y.hashCode();
      }
    }
  }

  public static void main(String[] args) {
    test(new WrappedBoolean(null));
  }
}
