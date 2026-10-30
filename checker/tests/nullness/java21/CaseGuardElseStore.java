// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that the facts that hold when a case guard is false are not assumed when the case does not
// match, because the case also fails to match when its pattern does not match, in which case the
// guard is not evaluated.

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CaseGuardElseStore {

  @EnsuresNonNullIf(expression = "#1", result = true)
  static boolean isNN(@Nullable Object o) {
    return o != null;
  }

  @EnsuresNonNullIf(expression = "#1", result = false)
  static boolean isNull(@Nullable Object o) {
    return o == null;
  }

  void guardFalseOrPatternFails(Object sel, @Nullable Object o) {
    switch (sel) {
      case String s when isNull(o) -> {}
      default -> {
        // :: error: [dereference.of.nullable]
        o.hashCode();
      }
    }
  }

  void guardFalseOrPatternFailsNextCase(Object sel, @Nullable Object o) {
    switch (sel) {
      case String s when isNull(o) -> {}
      case Integer i -> {
        // :: error: [dereference.of.nullable]
        o.hashCode();
      }
      default -> {}
    }
  }

  void guardTrue(Object sel, @Nullable Object o) {
    switch (sel) {
      case String s when isNN(o) -> {
        o.hashCode();
      }
      default -> {}
    }
  }
}
