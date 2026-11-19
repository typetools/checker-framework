import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
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

  static class Base {}

  static class Sub extends Base {}

  // An unannotated binding variable has the qualifiers of the selector.  The selector's
  // initialization frame is not the default, so the test fails if the frame is lost.
  void unknownInitialization(@UnknownInitialization(Base.class) Base o) {
    switch (o) {
      case Sub s -> {
        @UnknownInitialization(Base.class) Sub t = s;
        // :: error: (assignment)
        @Initialized Sub u = s;
      }
      default -> {}
    }
  }

  // A type pattern nested in a record pattern that is not unconditional for its record component
  // does not match null, so the binding variable is non-null.
  void nested(Box b) {
    if (b instanceof Box(String s)) {
      s.length();
    }
  }
}
