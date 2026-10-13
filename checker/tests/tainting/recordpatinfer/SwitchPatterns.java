// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Record pattern inference happens in every context in which values of a type T are matched against
// a record pattern (JLS 26 section 18.5.5), not just `instanceof`.  These tests cover switch
// statements, switch expressions, guarded patterns, and nested patterns, where T comes from the
// selector expression or from an enclosing pattern's inferred component type.
//
// The `Pair` example is adapted from JEP 440: Record Patterns (https://openjdk.org/jeps/440),
// section "Record patterns and exhaustive switch".

package recordpatinfer;

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class SwitchPatterns {

  record Box<T>(T t) {}

  sealed interface Shape<T> permits Circle, Square {}

  record Circle<T>(T center, double radius) implements Shape<T> {}

  record Square<T>(T corner, double side) implements Shape<T> {}

  /** T comes from the selector expression's type. */
  static void switchStatement(Box<@Untainted String> b) {
    switch (b) {
      case Box(var s) -> {
        @Untainted String ss = s;
      }
    }
  }

  static void switchStatementTainted(Box<@Tainted String> b) {
    switch (b) {
      case Box(var s) -> {
        // :: error: (assignment)
        @Untainted String ss = s;
      }
    }
  }

  /** A switch expression, where the result type also depends on the inferred component type. */
  static @Untainted String switchExpression(Box<@Untainted String> b) {
    return switch (b) {
      case Box(var s) -> s;
    };
  }

  static @Untainted String switchExpressionTainted(Box<@Tainted String> b) {
    return switch (b) {
      // :: error: (switch.expression)
      case Box(var s) -> s;
    };
  }

  /** Exhaustive switch over a sealed generic hierarchy: T is Shape<@Untainted String>. */
  static void sealedHierarchy(Shape<@Untainted String> shape) {
    switch (shape) {
      case Circle(var center, var radius) -> {
        @Untainted String c = center;
      }
      case Square(var corner, var side) -> {
        @Untainted String c = corner;
      }
    }
  }

  static void sealedHierarchyTainted(Shape<@Tainted String> shape) {
    switch (shape) {
      case Circle(var center, var radius) -> {
        // :: error: (assignment)
        @Untainted String c = center;
      }
      case Square(var corner, var side) -> {
        // :: error: (assignment)
        @Untainted String c = corner;
      }
    }
  }

  /** The pattern variable is used in the `when` guard, before the case body. */
  static void guardedPattern(Box<@Untainted String> b) {
    switch (b) {
      case Box(var s) when requiresUntainted(s) -> {}
      default -> {}
    }
  }

  static void guardedPatternTainted(Box<@Tainted String> b) {
    switch (b) {
      // :: error: (argument)
      case Box(var s) when requiresUntainted(s) -> {}
      default -> {}
    }
  }

  static boolean requiresUntainted(@Untainted String s) {
    return true;
  }

  /** Nested inference inside a switch case. */
  static void nestedInSwitch(Box<@Untainted Box<@Untainted String>> bbs) {
    switch (bbs) {
      case Box(Box(var s)) -> {
        @Untainted String ss = s;
      }
    }
  }

  /**
   * The selector's type is Object, so nothing is known about the type argument, but the case labels
   * still have to type-check.
   */
  static void objectSelector(@Tainted Object o) {
    switch (o) {
      case Box(var s) -> {
        // :: error: (assignment)
        @Untainted Object bad = s;
      }
      default -> {}
    }
  }
}
