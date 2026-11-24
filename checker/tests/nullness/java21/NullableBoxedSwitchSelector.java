import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// A switch with a `case null` label does not unbox its selector, so the selector remains nullable
// after the switch.
public class NullableBoxedSwitchSelector {

  <T extends @Nullable Integer> void typeVar(@Nullable T t) {
    switch (t) {
      case null -> {}
      default -> {}
    }
    // :: error: [dereference.of.nullable]
    t.toString();
  }

  void boxed(@Nullable Integer i) {
    switch (i) {
      case null -> {}
      default -> {}
    }
    // :: error: [dereference.of.nullable]
    i.toString();
  }

  <T extends @Nullable Integer> void typeVarNullDefault(@Nullable T t) {
    switch (t) {
      case 1 -> {}
      case null, default -> {}
    }
    // :: error: [dereference.of.nullable]
    t.toString();
  }

  <T extends @Nullable Integer> void typeVarNullCaseBody(@Nullable T t) {
    switch (t) {
      case 1 -> t.toString();
      // :: error: [dereference.of.nullable]
      case null -> t.toString();
      default -> t.toString();
    }
  }
}
