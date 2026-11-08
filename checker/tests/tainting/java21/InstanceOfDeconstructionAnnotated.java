// @below-java21-jdk-skip-test

// Test that an explicitly-annotated binding variable of a deconstruction pattern is checked against
// the type of its record component.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class InstanceOfDeconstructionAnnotated {

  record Pair(@Tainted String x, @Tainted String y) {}

  static void isUntainted(@Untainted String a) {}

  void explicitlyAnnotated(Object o) {
    // :: error: [assignment]
    if (o instanceof Pair(@Untainted String a, String b)) {
      isUntainted(a);
    }
  }
}
