// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that a case does not refine a selector expression that a guard of an earlier case may have
// changed.  The case labels are tested against the value that the selector had before the switch.

import org.checkerframework.checker.nullness.qual.Nullable;

public class SwitchSelectorChangedByGuard {

  sealed interface Shape permits Circle, Square {}

  record Circle() implements Shape {}

  record Square() implements Shape {}

  @Nullable Shape shape;

  @Nullable Object g;

  boolean clearShape() {
    this.shape = null;
    return false;
  }

  boolean clearG() {
    g = null;
    return false;
  }

  void guardChangesField() {
    this.shape = new Circle();
    switch (this.shape) {
      case Circle c when clearShape() -> {}
      case Shape t -> {
        // :: error: [dereference.of.nullable]
        this.shape.toString();
      }
    }
  }

  void guardChangesFieldLastCase() {
    this.shape = new Circle();
    switch (this.shape) {
      case Circle c when clearShape() -> {}
      case Circle c -> {}
      case Square q -> {
        // :: error: [dereference.of.nullable]
        this.shape.toString();
      }
    }
  }

  int guardChangesFieldLaterCases() {
    if (shape == null) {
      return 0;
    }
    return switch (shape) {
      case Circle c when clearShape() -> 1;
      // :: error: [dereference.of.nullable]
      case Square s -> shape.hashCode();
      // :: error: [dereference.of.nullable]
      case Circle c -> shape.hashCode();
    };
  }

  // Without a guard, the selector field is refined.
  void noGuard() {
    this.shape = new Circle();
    switch (this.shape) {
      case Circle c -> {}
      case Shape t -> {
        this.shape.toString();
      }
    }
  }

  // A guard cannot change a local variable that is the selector expression.
  String localSelectorAfterGuard(@Nullable Shape s) {
    return switch (s) {
      case null -> "";
      case Circle c when clearG() -> "";
      case Circle c -> s.toString();
      case Square q -> s.toString();
    };
  }
}
