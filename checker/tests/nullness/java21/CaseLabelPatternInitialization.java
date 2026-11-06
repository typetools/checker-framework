import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
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

  // A binding variable nested in a record pattern is not defaulted to top.
  void nested(Box b) {
    if (b instanceof Box(String s)) {
      s.length();
    }
  }
}
