// A switch expression whose results are boxed booleans is interned, but one whose results are
// boxed ints is not.

// @below-java17-jdk-skip-test

import org.checkerframework.checker.interning.qual.Interned;

public class SwitchExpressionBoxing {
  enum Kind {
    A,
    B
  }

  @Interned Boolean equalTo(Kind k, int x, int y) {
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

  @Interned Integer internedSum(Kind k, int x, int y) {
    // :: error: [return]
    return switch (k) {
      case A -> x + y;
      case B -> x - y;
    };
  }
}
