import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// A switch with a null label does not unbox its selector, so the selector may still be null after
// the switch.
public class NullCaseBoxedSelector {

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
      case 1 -> {}
      default -> {}
    }
    // :: error: [dereference.of.nullable]
    i.toString();
  }
}
