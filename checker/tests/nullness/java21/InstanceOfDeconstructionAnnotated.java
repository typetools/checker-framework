// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that an explicitly-annotated binding variable of a deconstruction pattern is checked against
// the type of its record component.

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class InstanceOfDeconstructionAnnotated {

  record NPoint(@Nullable String name, int y) {}

  record NBox(@Nullable Object o) {}

  record Strings(List<@Nullable String> l) {}

  void explicitlyAnnotated(Object o) {
    // :: error: [assignment]
    if (o instanceof NPoint(@NonNull String n, int y)) {
      n.length();
    }
  }

  void typeTest(Object o) {
    // A nested type pattern that is not unconditional does not match null, so it is checked
    // against the component's type with the non-null qualifier.
    if (o instanceof NBox(@NonNull String s)) {
      s.length();
    }
  }

  void typeArguments(Object o) {
    // :: error: [assignment]
    if (o instanceof Strings(List<String> l)) {
      l.get(0).length();
    }
  }
}
