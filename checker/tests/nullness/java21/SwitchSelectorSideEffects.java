// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that a case label refines a field that is the switch selector expression, unless a guard or
// a deconstruction pattern that might have side effects runs after the switch evaluates the
// selector expression.

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

public class SwitchSelectorSideEffects {

  sealed interface Shape permits Circle, Square {}

  record Circle(int radius) implements Shape {}

  record Square(int side) implements Shape {
    // An accessor that reassigns the selector expression.
    @Override
    public int side() {
      selector = null;
      return side;
    }
  }

  static @Nullable Shape selector;

  @Pure
  static boolean isPositive(int i) {
    return i > 0;
  }

  static boolean reset() {
    selector = null;
    return false;
  }

  void pureGuard() {
    // :: error: (switching.nullable)
    switch (selector) {
      case Circle c when isPositive(c.radius()) -> {}
      case Circle c -> selector.toString();
      case Square s -> selector.toString();
    }
  }

  void impureGuard() {
    // :: error: (switching.nullable)
    switch (selector) {
      case Circle c when reset() -> {}
      case Circle c ->
          // :: error: (dereference.of.nullable)
          selector.toString();
      case Square s ->
          // :: error: (dereference.of.nullable)
          selector.toString();
    }
  }

  void pureDeconstruction() {
    // :: error: (switching.nullable)
    switch (selector) {
      case Circle(int radius) -> {}
      case Square s -> selector.toString();
    }
  }

  void impureDeconstruction() {
    // :: error: (switching.nullable)
    switch (selector) {
      case Square(int side) -> {}
      case Circle c ->
          // :: error: (dereference.of.nullable)
          selector.toString();
    }
  }
}
