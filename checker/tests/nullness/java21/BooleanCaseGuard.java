// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that a case guard of type Boolean is non-null only where the guard was evaluated, because
// evaluating the guard unboxes it.

import org.checkerframework.checker.nullness.qual.Nullable;

// This test is about the refinement of the guard, not about whether the guard might be null.
// Suppressing "unneeded.suppression" keeps this test independent of whether the Nullness Checker
// checks a case guard for nullness:  without that check, "condition.nullable" is never issued.
@SuppressWarnings({"nullness:condition.nullable", "nullness:unneeded.suppression"})
public class BooleanCaseGuard {

  sealed interface Shape permits Circle, Square {}

  record Circle() implements Shape {}

  record Square() implements Shape {}

  void nullableBooleanGuard(Object sel, @Nullable Boolean b) {
    switch (sel) {
      case String s when b -> {
        b.hashCode();
      }
      default -> {
        // The guard is not evaluated if the pattern does not match.
        // :: error: [dereference.of.nullable]
        b.hashCode();
      }
    }
  }

  // The pattern matches every value of the selector, so the default case is reached only when the
  // guard is false, which means that the guard was evaluated.
  void nullableBooleanGuardPatternAlwaysMatches(String sel, @Nullable Boolean b) {
    switch (sel) {
      case String s when b -> {}
      default -> {
        b.hashCode();
      }
    }
  }

  void nullableBooleanGuardNextCase(Object sel, @Nullable Boolean b) {
    switch (sel) {
      case String s when b -> {}
      case Integer i -> {
        // :: error: [dereference.of.nullable]
        b.hashCode();
      }
      default -> {}
    }
  }

  void nullableBooleanGuardColon(Object sel, @Nullable Boolean b) {
    switch (sel) {
      case String s
      when b:
        b.hashCode();
        break;
      default:
        // :: error: [dereference.of.nullable]
        b.hashCode();
    }
  }

  int nullableBooleanGuardSwitchExpression(Object sel, @Nullable Boolean b) {
    return switch (sel) {
      case String s when b -> b.hashCode();
      // :: error: [dereference.of.nullable]
      default -> b.hashCode();
    };
  }

  int nullableBooleanGuardLastCase(Shape shape, @Nullable Boolean b) {
    return switch (shape) {
      case Circle c -> 1;
      case Square s -> 2;
      case Shape s when b -> b.hashCode();
    };
  }
}
