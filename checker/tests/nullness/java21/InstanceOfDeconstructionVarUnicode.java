// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that a binding variable of a deconstruction pattern that is declared using var has the type
// arguments of its record component, when the source code spells var with a Unicode escape.
// google-java-format cannot format this file, so buildSrc/src/main/groovy/cf-spotless.gradle
// excludes it from formatting.

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class InstanceOfDeconstructionVarUnicode {

  record Strings(List<@Nullable String> l) {}

  void unicodeEscape(Object o) {
    if (o instanceof Strings(\u0076ar l)) {
      // :: error: [dereference.of.nullable]
      l.get(0).length();
    }
  }
}
