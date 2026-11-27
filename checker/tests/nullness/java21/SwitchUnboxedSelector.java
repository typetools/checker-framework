import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// A switch with a `case null` label does not unbox its selector, so a null selector does not throw
// a NullPointerException before the cases are tested.
public class SwitchUnboxedSelector {

  static String boxedCaseNull(@Nullable Integer i) {
    return switch (i) {
      // :: error: [dereference.of.nullable]
      case null -> i.toString();
      default -> "default";
    };
  }

  static String boxedPattern(@Nullable Integer i) {
    return switch (i) {
      case Integer j -> "Integer";
      // :: error: [dereference.of.nullable]
      case null -> i.toString();
    };
  }

  // A switch with a pattern label but no `case null` label unboxes its selector, which throws a
  // NullPointerException if the selector is null.  So, the selector is non-null in every case and
  // after the switch.
  static String boxedPatternNoCaseNull(@Nullable Integer i) {
    String s;
    // :: error: [switching.nullable]
    switch (i) {
      case Integer j -> s = i.toString();
    }
    return s + i.toString();
  }

  static <T extends @Nullable Integer> String typeVarCaseNull(T t) {
    return switch (t) {
      // :: error: [dereference.of.nullable]
      case null -> t.toString();
      default -> "default";
    };
  }

  static <T extends @Nullable Integer> String typeVarPattern(T t) {
    return switch (t) {
      case Integer j -> "Integer";
      // :: error: [dereference.of.nullable]
      case null -> t.toString();
    };
  }
}
