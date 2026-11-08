// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that a binding variable of a deconstruction pattern that is declared using var has the type
// arguments of its record component.

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class InstanceOfDeconstructionVar {

  record Strings(List<@Nullable String> l) {}

  void typeArguments(Object o) {
    if (o instanceof Strings(var l)) {
      // :: error: [dereference.of.nullable]
      l.get(0).length();
    }
  }
}
