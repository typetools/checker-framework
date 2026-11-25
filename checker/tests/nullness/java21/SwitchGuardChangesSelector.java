import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// A guard can change the value of a selector field.  A case label is matched against the saved
// value of the selector, so a case label does not refine the selector field after a guard has run.
public class SwitchGuardChangesSelector {

  @Nullable Integer boxedField;

  @Nullable String stringField;

  boolean clear() {
    boxedField = null;
    stringField = null;
    return false;
  }

  void boxed() {
    switch (boxedField) {
      case Integer n when clear() -> {}
      case null -> {}
      default -> {
        // :: error: [dereference.of.nullable]
        boxedField.toString();
      }
    }
  }

  void string() {
    switch (stringField) {
      case String s when clear() -> {}
      case null -> {}
      default -> {
        // :: error: [dereference.of.nullable]
        stringField.toString();
      }
    }
  }

  void noGuard() {
    switch (stringField) {
      case null -> {}
      default -> stringField.toString();
    }
  }

  void localSelector(@Nullable String local) {
    switch (local) {
      case String s when clear() -> {}
      case null -> {}
      default -> local.toString();
    }
  }
}
