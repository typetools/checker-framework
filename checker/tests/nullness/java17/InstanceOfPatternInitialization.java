// @below-java17-jdk-skip-test

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class InstanceOfPatternInitialization {

  // A type pattern in an instanceof does not match null, so the binding variable is non-null.
  void nullable(@Nullable Object o) {
    if (o instanceof String s) {
      s.length();
    }
  }

  // An explicitly @NonNull binding variable is safe even when the expression is @Nullable.
  void nonNullExplicit(@Nullable Object o) {
    if (o instanceof @NonNull String s) {
      s.length();
    }
  }

  // An unannotated binding variable has the qualifiers of the expression that instanceof tests.
  void unknownInitialization(@UnknownInitialization Object o) {
    if (o instanceof String s) {
      @UnknownInitialization String t = s;
      // :: error: (assignment)
      @Initialized String u = s;
    }
  }

  void initializedExplicit(@UnknownInitialization Object o) {
    // :: warning: (instanceof.pattern.unsafe)
    if (o instanceof @Initialized String s) {
      @Initialized String t = s;
    }
  }

  static class Sub extends InstanceOfPatternInitialization {}

  InstanceOfPatternInitialization() {
    if (this instanceof Sub s) {
      @UnknownInitialization Sub t = s;
      // :: error: (method.invocation)
      s.hashCode();
    }
  }
}
