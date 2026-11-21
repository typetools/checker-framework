// @below-java21-jdk-skip-test

// Test that the binding variables of a deconstruction pattern have the qualifiers of the record
// components, not those of the expression being tested.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class DeconstructionComponentQualifiers {

  record Pair(@Tainted String x, @Tainted String y) {}

  record UPair(@Untainted String x, @Untainted String y) {}

  record Wrapper(@Tainted Object o) {}

  static void isUntainted(@Untainted String a) {}

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
}
