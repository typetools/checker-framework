// @below-java21-jdk-skip-test

// Test that the variable of a type pattern in a switch is refined by the type of the selector
// expression, as for instanceof.

import org.checkerframework.checker.tainting.qual.Untainted;

public class SwitchTypePattern {

  static void isUntainted(@Untainted String a) {}

  void switchStatement(@Untainted Object o) {
    switch (o) {
      case String s -> isUntainted(s);
      default -> {}
    }
  }

  int switchExpression(@Untainted Object o) {
    return switch (o) {
      case Integer i -> i;
      case String s -> {
        isUntainted(s);
        yield 0;
      }
      default -> 1;
    };
  }

  void instanceOf(@Untainted Object o) {
    if (o instanceof String s) {
      isUntainted(s);
    }
  }

  void tainted(Object o) {
    switch (o) {
      case String s ->
          // :: error: [argument]
          isUntainted(s);
      default -> {}
    }
  }
}
