// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Tests for the qualifiers of type patterns nested in a record pattern. A nested type pattern that
// is unconditional for its record component always matches, so binding it is an assignment. One
// that is not unconditional is checked like a type pattern in an instanceof.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class NestedTypePatterns {

  record Box<T>(T t) {}

  record ObjectBox(Object o) {}

  static void unconditional(Box<@Tainted String> b) {
    if (b instanceof Box(String s)) {
      // :: error: (assignment)
      @Untainted String ss = s;
    }
  }

  static void unconditionalExplicitTop(Box<@Untainted String> b) {
    if (b instanceof Box(@Tainted String s)) {}
  }

  static void unconditionalExplicitBad(Box<@Tainted String> b) {
    // :: error: (assignment)
    if (b instanceof Box(@Untainted String s)) {}
  }

  static void conditionalExplicitBad(ObjectBox b) {
    // :: warning: (instanceof.pattern.unsafe)
    if (b instanceof ObjectBox(@Untainted String s)) {}
  }

  static void conditionalExplicitGood(Box<@Untainted Object> b) {
    if (b instanceof Box(@Untainted String s)) {}
  }
}
