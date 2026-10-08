import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test
public class TypeVarSwitchSelector {

  // A switch over a selector whose type is a type variable is a pattern switch, so the selector is
  // not unboxed and the `case null` branch is reachable.
  <T extends @Nullable Integer> String patternSwitch(T t) {
    return switch (t) {
      // :: error: [dereference.of.nullable]
      case null -> t.toString();
      case Integer i -> i.toString();
    };
  }
}
