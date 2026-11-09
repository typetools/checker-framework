import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// A type pattern in a case label is not yet checked against the selector.
// @skip-test
public class CaseLabelPatternExplicit {

  // An explicitly annotated binding variable is checked against the selector, as in
  // InstanceOfPatternInitialization.initializedExplicit.  The name of the message key is a
  // placeholder.
  void initializedExplicit(@UnknownInitialization Object o) {
    switch (o) {
      // :: warning: (case.pattern.unsafe)
      case @Initialized String s -> {
        @Initialized String t = s;
      }
      default -> {}
    }
  }
}
