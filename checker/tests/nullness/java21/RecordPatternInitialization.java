import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test
public class RecordPatternInitialization {

  record NullableBox(@Nullable Object o) {}

  record UnknownInitBox(@UnknownInitialization Object o) {}

  // `String s` is not unconditional for Object, so it does not match null, and the binding variable
  // is non-null.
  void nullableComponent(NullableBox box) {
    if (box instanceof NullableBox(String s)) {
      s.length();
    }
  }

  // An unannotated binding variable has the qualifiers of the record component.
  void unknownInitializationComponent(UnknownInitBox box) {
    if (box instanceof UnknownInitBox(String s)) {
      @UnknownInitialization String t = s;
      // :: error: (assignment)
      @Initialized String u = s;
    }
  }

  void initializedExplicit(UnknownInitBox box) {
    // :: warning: (instanceof.pattern.unsafe)
    if (box instanceof UnknownInitBox(@Initialized String s)) {
      @Initialized String t = s;
    }
  }

  void unknownInitializationVar(UnknownInitBox box) {
    if (box instanceof UnknownInitBox(var o)) {
      // :: error: (assignment)
      @Initialized Object t = o;
    }
  }
}
