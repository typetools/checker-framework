import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test
public class TypeVarSwitchSelector {

  // A switch with a `case null` label does not unbox its selector, so the `case null` branch is
  // reachable.
  <T extends @Nullable Integer> String patternSwitch(T t) {
    return switch (t) {
      // :: error: [dereference.of.nullable]
      case null -> t.toString();
      case Integer i -> i.toString();
    };
  }

  // A switch without a `case null` label throws NullPointerException if its selector is null, so
  // the selector is non-null afterward.
  <T extends @Nullable Integer> @NonNull Integer defaultOnly(T t) {
    // :: error: [switching.nullable]
    switch (t) {
      default -> {}
    }
    return t;
  }

  <T extends @Nullable Integer> @NonNull Integer patternWithoutNull(T t) {
    // :: error: [switching.nullable]
    switch (t) {
      case Integer i -> {}
    }
    return t;
  }
}
