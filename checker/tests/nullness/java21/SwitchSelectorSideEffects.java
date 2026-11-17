// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that a case label refines the switch selector expression, unless a guard or a deconstruction
// pattern that might have side effects runs after the switch evaluates the selector expression and
// might have changed the selector expression.

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

  @Pure
  static @Nullable Shape getSelector() {
    return selector;
  }

  static boolean reset() {
    selector = null;
    return false;
  }

  // Without a guard or a deconstruction pattern, a case label refines the selector field.
  void noGuard() {
    // :: error: (switching.nullable)
    switch (selector) {
      case Circle c -> {}
      case Shape t -> selector.toString();
    }
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

  // A guard cannot change a local variable that is the selector expression.
  void impureGuardLocalSelector(@Nullable Shape local) {
    switch (local) {
      case null -> {}
      case Circle c when reset() -> {}
      case Circle c -> local.toString();
      case Square s -> local.toString();
    }
  }

  // A guard might change the result of a method call that is the selector expression.
  void impureGuardMethodCallSelector() {
    if (getSelector() == null) {
      return;
    }
    switch (getSelector()) {
      case Circle c when reset() -> {}
      case Circle c ->
          // :: error: (dereference.of.nullable)
          getSelector().toString();
      case Square s ->
          // :: error: (dereference.of.nullable)
          getSelector().toString();
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

  // A record accessor that a case's own record pattern invokes might change the selector, even
  // though an earlier `case null` refined it.
  void impureDeconstructionSameCase() {
    switch (selector) {
      case null -> {}
      case Square(int side) ->
          // :: error: (dereference.of.nullable)
          selector.toString();
      case Circle c -> {}
    }
  }

  // Without a deconstruction pattern, the refinement by an earlier `case null` persists.
  void noDeconstructionAfterCaseNull() {
    switch (selector) {
      case null -> {}
      case Square s -> selector.toString();
      case Circle c -> selector.toString();
    }
  }

  record Inner() {}

  record Outer(@Nullable Inner inner) {
    // A record pattern invokes this accessor, even if the pattern does not match.
    @Override
    public @Nullable Inner inner() {
      outerSelector = null;
      return inner;
    }
  }

  static @Nullable Outer outerSelector;

  // The sole case is exhaustive, but Outer(null) is a remainder that it does not match, so the
  // switch throws MatchException after the accessor has changed the selector.
  void impureDeconstructionBeforeMatchException() {
    outerSelector = new Outer(null);
    try {
      switch (outerSelector) {
        case Outer(Inner()) -> {}
      }
    } catch (RuntimeException e) {
      // :: error: (dereference.of.nullable)
      outerSelector.hashCode();
    }
  }
}
