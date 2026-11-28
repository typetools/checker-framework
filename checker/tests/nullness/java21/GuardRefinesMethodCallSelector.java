// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that a fact that a guard establishes about a method-call selector expression, after the
// guard's side effect, reaches the body of a later case.

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

public class GuardRefinesMethodCallSelector {

  sealed interface Shape permits Circle, Square {}

  record Circle(int radius) implements Shape {}

  record Square(int side) implements Shape {}

  static @Nullable Shape selector;

  @Pure
  static @Nullable Shape current() {
    return selector;
  }

  static boolean sideEffect() {
    selector = null;
    return false;
  }

  // The guard's false edge establishes that current() is non-null after the side effect.  The last
  // case's labels are untested.
  void sideEffectThenTest() {
    switch (current()) {
      case null -> {}
      case Square s -> {}
      case Circle c when sideEffect() || current() == null -> {}
      case Circle c -> current().toString();
    }
  }

  // The same, with a last case whose labels are tested.
  void sideEffectThenTestTestedLabels() {
    switch (current()) {
      case null -> {}
      case Circle c when sideEffect() || current() == null -> {}
      case Circle c -> current().toString();
      default -> {}
    }
  }

  // The guard's side effect might change current(), so a fact about current() from before the
  // guard does not hold after it.
  void sideEffectOnly() {
    switch (current()) {
      case null -> {}
      case Square s -> {}
      case Circle c when sideEffect() -> {}
      case Circle c ->
          // :: error: (dereference.of.nullable)
          current().toString();
    }
  }
}
