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

  // The binding variables of a deconstruction pattern are refined to the types of the record
  // components.
  record MixedPair(@Untainted String x, @Tainted String y) {}

  void deconstructMixed(@Tainted Object o) {
    if (o instanceof MixedPair(String a, String b)) {
      isUntainted(a);
      // :: error: [argument]
      isUntainted(b);
    }
  }

  record Nested(MixedPair mp, @Tainted String z) {}

  void deconstructNested(@Tainted Object o) {
    if (o instanceof Nested(MixedPair(String a, String b), String c)) {
      isUntainted(a);
      // :: error: [argument]
      isUntainted(b);
      // :: error: [argument]
      isUntainted(c);
    }
  }

  // The types of generic record components are those of the pattern's type arguments.
  record Box<T>(T value) {}

  void deconstructGeneric(@Untainted Object o) {
    if (o instanceof Box<@Untainted String>(String a)) {
      isUntainted(a);
    }
  }

  void deconstructGenericTainted(@Untainted Object o) {
    if (o instanceof Box<@Tainted String>(String a)) {
      // :: error: [argument]
      isUntainted(a);
    }
  }

  // The variable of a binding pattern is still refined to the type of the expression.
  void bindingPattern(@Untainted Object o) {
    if (o instanceof String s) {
      isUntainted(s);
    }
  }
}
