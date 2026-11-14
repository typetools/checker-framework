import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// A switch with a `case null` label does not throw NullPointerException if its selector is null, so
// the selector might be null in the cases and after the switch.
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

  @NonNull Integer boxedAfter(@Nullable Integer i) {
    switch (i) {
      case null -> {}
      default -> {}
    }
    // :: error: [return]
    return i;
  }

  // In a case other than `case null`, the selector is non-null.

  void boxedNonNullCases(@Nullable Integer i) {
    switch (i) {
      case 1 -> i.toString();
      case null -> {}
      default -> i.toString();
    }
  }

  int boxedSwitchExpression(@Nullable Integer i) {
    return switch (i) {
      case null -> 0;
      default -> i;
    };
  }

  int boxedSwitchExpressionNullCase(@Nullable Integer i) {
    return switch (i) {
      // :: error: [unboxing.of.nullable]
      case null -> i;
      default -> 0;
    };
  }

  // The selector's type is a type variable whose bound is a primitive wrapper.

  <T extends @Nullable Integer> void typeVar(T t) {
    switch (t) {
      case null -> {
        // :: error: [dereference.of.nullable]
        t.toString();
      }
      default -> {}
    }
  }

  <T extends @Nullable Integer> @NonNull T typeVarAfter(T t) {
    switch (t) {
      case null -> {}
      default -> {}
    }
    // :: error: [return]
    return t;
  }
}
