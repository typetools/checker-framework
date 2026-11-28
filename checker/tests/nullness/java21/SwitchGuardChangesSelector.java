import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// A guard can change the value of a selector field, and so can a record accessor method that a
// record pattern calls.  A case label is matched against the saved value of the selector, so a case
// label does not refine the selector field after a guard or a record pattern has run.
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

  static @Nullable Object staticField;

  record ClearingRecord(Object component) {
    @Override
    public Object component() {
      staticField = null;
      return component;
    }
  }

  void recordPattern() {
    switch (staticField) {
      case ClearingRecord(String s) -> {}
      case null -> {}
      default -> {
        // :: error: [dereference.of.nullable]
        staticField.toString();
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

  // A guard cannot assign a local variable, so a case label refines a local assignment target.
  void assignmentSelectorLocalTarget() {
    @Nullable String saved;
    switch (saved = stringField) {
      case String s when clear() -> {}
      case null -> {}
      default -> {
        saved.toString();
        // :: error: [dereference.of.nullable]
        stringField.toString();
      }
    }
  }

  // A guard can assign a field, so a case label does not refine a field assignment target.
  void assignmentSelectorFieldTarget(@Nullable String local) {
    switch (stringField = local) {
      case String s when clear() -> {}
      case null -> {}
      default -> {
        local.toString();
        // :: error: [dereference.of.nullable]
        stringField.toString();
      }
    }
  }
}
