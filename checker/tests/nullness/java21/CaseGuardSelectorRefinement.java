// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test the refinement of the switch selector expression after a guard, and test that only the
// guard's true store reaches the body of the last case of an exhaustive switch.

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CaseGuardSelectorRefinement {

  sealed interface Shape permits Circle, Square {}

  record Circle() implements Shape {}

  record Square() implements Shape {}

  @Nullable Shape f;

  @Nullable Object g;

  boolean clearF() {
    f = null;
    return false;
  }

  boolean sideEffect() {
    g = null;
    return false;
  }

  @EnsuresNonNullIf(expression = "g", result = true)
  boolean gIsNonNull() {
    return g != null;
  }

  // If the guard is false, the switch throws a MatchException, so the body sees only the facts
  // that hold when the guard is true.
  int lastCaseGuardTrueStore(Shape shape) {
    return switch (shape) {
      case Circle c -> 1;
      case Square s -> 2;
      case Shape s when gIsNonNull() -> g.hashCode();
    };
  }

  // The guard of an earlier case might change a field that is the selector expression.
  int selectorFieldClearedByGuard() {
    if (f == null) {
      return 0;
    }
    return switch (f) {
      case Circle c when clearF() -> 1;
      // :: error: [dereference.of.nullable]
      case Square s -> f.hashCode();
      // :: error: [dereference.of.nullable]
      case Circle c -> f.hashCode();
    };
  }

  // A guard cannot change a local variable that is the selector expression.
  String localSelectorAfterGuard(@Nullable Shape shape) {
    return switch (shape) {
      case null -> "";
      case Circle c when sideEffect() -> "";
      case Circle c -> shape.toString();
      case Square s -> shape.toString();
    };
  }
}
