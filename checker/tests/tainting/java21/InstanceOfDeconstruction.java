// @below-java21-jdk-skip-test

// Test that the binding variables of a deconstruction pattern have the types of the record
// components, not the type of the expression being tested.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class InstanceOfDeconstruction {

  record Pair(@Tainted String x, @Tainted String y) {}

  record UPair(@Untainted String x, @Untainted String y) {}

  record Wrapper(@Tainted Object o) {}

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

  void deconstructUntainted(Object o) {
    if (o instanceof UPair(String a, var b)) {
      isUntainted(a);
      isUntainted(b);
    }
  }

  void deconstructNested(Object o) {
    if (o instanceof Wrapper(UPair(String a, String b))) {
      isUntainted(a);
    }
  }

  void deconstructSwitch(Object o) {
    switch (o) {
      case UPair(String a, String b) -> isUntainted(a);
      case Pair(String a, String b) ->
          // :: error: [argument]
          isUntainted(a);
      default -> {}
    }
  }

  void explicitlyAnnotated(Object o) {
    // :: error: [assignment]
    if (o instanceof Pair(@Untainted String a, String b)) {
      isUntainted(a);
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

  void deconstructNestedMixed(@Tainted Object o) {
    if (o instanceof Nested(MixedPair(String a, String b), String c)) {
      isUntainted(a);
      // :: error: [argument]
      isUntainted(b);
      // :: error: [argument]
      isUntainted(c);
    }
  }

  // The variable of a binding pattern is still refined to the type of the expression.
  void bindingPattern(@Untainted Object o) {
    if (o instanceof String s) {
      isUntainted(s);
    }
  }
}
