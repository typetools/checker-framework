import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// A switch with a `case null` label does not throw NullPointerException if its selector is null, so
// the selector may be null in the cases and after the switch.
public class SwitchSelectorCaseNull {

  void boxed(@Nullable Integer i) {
    switch (i) {
      case null -> {
        // :: error: [dereference.of.nullable]
        i.toString();
      }
      default -> {}
    }
  }

  void boxedNullDefault(@Nullable Integer i) {
    switch (i) {
      case 1 -> {}
      case null, default -> {
        // :: error: [dereference.of.nullable]
        i.toString();
      }
    }
  }

  @NonNull String string(@Nullable String s) {
    switch (s) {
      case null -> {}
      default -> {}
    }
    // :: error: [return]
    return s;
  }

  @NonNull Integer boxedAfter(@Nullable Integer i) {
    switch (i) {
      case null -> {}
      default -> {}
    }
    // :: error: [return]
    return i;
  }
}
