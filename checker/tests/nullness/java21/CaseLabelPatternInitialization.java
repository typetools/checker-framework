import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test
public class CaseLabelPatternInitialization {

  record Box(@Nullable Object o) {}

  // A type pattern that is a case label does not match null, so the binding variable is non-null.
  void nullable(@Nullable Object o) {
    switch (o) {
      case String s -> s.length();
      case null, default -> {}
    }
  }

  // An unannotated binding variable has the qualifiers of the selector.
  void unknownInitialization(@UnknownInitialization Object o) {
    switch (o) {
      case String s -> {
        @UnknownInitialization String t = s;
        // :: error: (assignment)
        @Initialized String u = s;
      }
      default -> {}
    }
  }

  // An explicitly annotated binding variable is checked against the selector, as in
  // InstanceOfPatternInitialization.initializedExplicit.
  void initializedExplicit(@UnknownInitialization Object o) {
    switch (o) {
      // :: warning: (case.pattern.unsafe)
      case @Initialized String s -> {
        @Initialized String t = s;
      }
      default -> {}
    }
  }

  // A type pattern that is a case label does not match null, so an explicitly @NonNull binding
  // variable is safe.
  void nonNullExplicit(@Nullable Object o) {
    switch (o) {
      case @NonNull String s -> s.length();
      case null, default -> {}
    }
  }

  int switchExpression(@UnknownInitialization Object o) {
    return switch (o) {
      // :: warning: (case.pattern.unsafe)
      case @Initialized String s -> s.length();
      default -> 0;
    };
  }

  // A binding variable nested in a record pattern is not defaulted to top.
  void nested(Box b) {
    if (b instanceof Box(String s)) {
      s.length();
    }
  }
}
