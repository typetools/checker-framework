// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that a case guard is checked for nullness, as the condition of an if statement is.

import org.checkerframework.checker.nullness.qual.Nullable;

public class NullableCaseGuard {

  void nullableBooleanGuard(Object sel, @Nullable Boolean b) {
    switch (sel) {
      // :: error: [condition.nullable]
      case String s when b -> {
        b.hashCode();
      }
      default -> {}
    }
  }

  void nonNullBooleanGuard(Object sel, Boolean b) {
    switch (sel) {
      case String s when b -> {}
      default -> {}
    }
  }

  void primitiveGuard(Object sel, boolean b) {
    switch (sel) {
      case String s when b -> {}
      default -> {}
    }
  }
}
