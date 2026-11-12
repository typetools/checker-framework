// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that a case guard is checked for nullness, as the condition of an if statement is.

import org.checkerframework.checker.nullness.qual.Nullable;

public class NullableCaseGuard {

  sealed interface Shape permits Circle, Square {}

  record Circle() implements Shape {}

  record Square() implements Shape {}

  void nullableBooleanGuard(Object sel, @Nullable Boolean b) {
    switch (sel) {
      // :: error: [condition.nullable]
      case String s when b -> {}
      default -> {}
    }
  }

  void nullableBooleanGuardColon(Object sel, @Nullable Boolean b) {
    switch (sel) {
      case String s
      // :: error: [condition.nullable]
      when b:
        break;
      default:
    }
  }

  int nullableBooleanGuardSwitchExpression(Object sel, @Nullable Boolean b) {
    return switch (sel) {
      // :: error: [condition.nullable]
      case String s when b -> 1;
      default -> 2;
    };
  }

  int nullableBooleanGuardLastCase(Shape shape, @Nullable Boolean b) {
    return switch (shape) {
      case Circle c -> 1;
      case Square s -> 2;
      // :: error: [condition.nullable]
      case Shape s when b -> 3;
    };
  }

  int refinedBooleanGuardLastCase(Shape shape, @Nullable Boolean b) {
    if (b == null) {
      return 0;
    }
    return switch (shape) {
      case Circle c -> 1;
      case Square s -> 2;
      case Shape s when b -> 3;
    };
  }

  int conjunctionGuardLastCase(Shape shape, @Nullable Boolean b) {
    return switch (shape) {
      case Circle c -> 1;
      case Square s -> 2;
      case Shape s when b != null && b -> 3;
    };
  }

  int conditionalGuardLastCase(Shape shape, @Nullable Boolean b) {
    return switch (shape) {
      case Circle c -> 1;
      case Square s -> 2;
      case Shape s when (b != null ? b : false) -> 3;
    };
  }

  void refinedBooleanGuard(Object sel, @Nullable Boolean b) {
    if (b == null) {
      return;
    }
    switch (sel) {
      case String s when b -> {}
      default -> {}
    }
  }

  void conjunctionGuard(Object sel, @Nullable Boolean b) {
    switch (sel) {
      case String s when b != null && b -> {}
      default -> {}
    }
  }

  // After a guard is evaluated, it is non-null, as after the condition of an if statement.
  void nullableBooleanGuardBody(Object sel, @Nullable Boolean b) {
    switch (sel) {
      // :: error: [condition.nullable]
      case String s when b -> {
        b.toString();
      }
      default -> {}
    }
  }

  // The guard is evaluated only if the pattern matches, so it is not non-null after the switch.
  void nullableBooleanGuardAfterSwitch(Object sel, @Nullable Boolean b) {
    switch (sel) {
      // :: error: [condition.nullable]
      case String s when b -> {}
      default -> {}
    }
    // :: error: [dereference.of.nullable]
    b.toString();
  }

  // If the guard of the last case of an exhaustive switch is false, the switch throws a
  // MatchException rather than completing normally.
  void exhaustiveSwitchGuardedLastCase(Shape shape, boolean flag) {
    @Nullable Object o = null;
    switch (shape) {
      case Circle c -> o = "circle";
      case Square s -> o = "square";
      case Shape s when flag -> o = "shape";
    }
    o.toString();
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
