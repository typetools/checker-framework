// A switch expression whose results are boxed booleans is interned.

// @below-java17-jdk-skip-test

public class SwitchExpressionBoxing {
  enum Kind {
    A,
    B
  }

  Boolean equalTo(Kind k, int x, int y) {
    return switch (k) {
      case A -> x == y;
      case B -> x != y;
    };
  }

  Integer sum(Kind k, int x, int y) {
    return switch (k) {
      case A -> x + y;
      case B -> x - y;
    };
  }
}
