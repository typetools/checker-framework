// @below-java21-jdk-skip-test

// Test that the binding variables of a deconstruction pattern are not refined to the type of the
// expression being tested.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class InstanceOfDeconstruction {

  record Pair(@Tainted String x, @Tainted String y) {}

  static void isUntainted(@Untainted String a) {}

  void deconstruct(@Untainted Pair p) {
    if (p instanceof Pair(String a, String b)) {
      // :: error: [argument]
      isUntainted(a);
    }
  }

  void deconstructObject(@Untainted Object o) {
    if (o instanceof Pair(String a, String b)) {
      // :: error: [argument]
      isUntainted(b);
    }
  }
}
