// @below-java21-jdk-skip-test

// Test that an explicitly-annotated binding variable of a deconstruction pattern is checked against
// the type of its record component.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class InstanceOfDeconstructionAnnotated {

  record Pair(@Tainted String x, @Tainted String y) {}

  record Wrapper(@Tainted Object value) {}

  static void isUntainted(@Untainted String a) {}

  void explicitlyAnnotated(Object o) {
    // :: error: [assignment]
    if (o instanceof Pair(@Untainted String a, String b)) {
      isUntainted(a);
    }
  }

  // A pattern that is not unconditional for its component tests the component's Java type, as
  // instanceof does, and is checked like a type pattern in instanceof.
  void conditional(Object o) {
    // :: warning: [instanceof.pattern.unsafe]
    if (o instanceof Wrapper(@Untainted String s)) {
      isUntainted(s);
    }
  }
}
