// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

import org.checkerframework.checker.nullness.qual.Nullable;

// A guard of an earlier case might change the selector expression, so matching a later case says
// nothing about the selector expression's current value.
public class SwitchGuardChangesSelector {

  @Nullable String f;

  boolean clearF() {
    f = null;
    return false;
  }

  void guardClearsSelector() {
    switch ((@Nullable String) f) {
      case null -> {}
      case String s when clearF() -> {}
      case "match" -> {
        // :: error: (dereference.of.nullable)
        f.length();
      }
      default -> {}
    }
  }

  void guardClearsSelectorNoCast() {
    switch (f) {
      case null -> {}
      case String s when clearF() -> {}
      case "match" -> {
        // :: error: (dereference.of.nullable)
        f.length();
      }
      default -> {}
    }
  }

  // No earlier case has a guard, so refining the selector expression is sound.
  void noGuard() {
    switch ((@Nullable String) f) {
      case null -> {}
      case "match" -> f.length();
      default -> {}
    }
  }
}
