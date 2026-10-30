// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that a case guard is checked for nullness, as the condition of an if statement is, and that
// facts from the guard hold only where the guard was evaluated.

import org.checkerframework.checker.nullness.qual.Nullable;

public class NullableCaseGuard {

  @Nullable String f;

  sealed interface Shape permits Circle, Square {}

  record Circle() implements Shape {}

  record Square() implements Shape {}

  void nullableBooleanGuard(Object sel, @Nullable Boolean b) {
    switch (sel) {
      // :: error: [condition.nullable]
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

  void nullableBooleanGuardNextCase(Object sel, @Nullable Boolean b) {
    switch (sel) {
      // :: error: [condition.nullable]
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
      // :: error: [condition.nullable]
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
      // :: error: [condition.nullable]
      case String s when b -> b.hashCode();
      // :: error: [dereference.of.nullable]
      default -> b.hashCode();
    };
  }

  int nullableBooleanGuardLastCase(Shape shape, @Nullable Boolean b) {
    return switch (shape) {
      case Circle c -> 1;
      case Square s -> 2;
      // :: error: [condition.nullable]
      case Shape s when b -> b.hashCode();
    };
  }

  void guardElseStore(Object sel) {
    switch (sel) {
      case String s when f == null -> {}
      case Integer i -> {
        // The guard is not evaluated if the pattern does not match.
        // :: error: [dereference.of.nullable]
        f.hashCode();
      }
      default -> {}
    }
  }

  void guardThenStore(Object sel) {
    switch (sel) {
      case String s when f != null -> {
        f.hashCode();
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
