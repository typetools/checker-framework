import org.checkerframework.checker.interning.qual.Interned;

// A switch expression whose results are boxed booleans is interned.

// @below-java17-jdk-skip-test

public class SwitchExpressionBoxing {
  enum Kind {
    A,
    B
  }

  Boolean equalTo(Kind k, int x, int y) {
    Boolean result =
        switch (k) {
          case A -> x == y;
          case B -> x != y;
        };
    @Interned Boolean interned = result;
    return interned;
  }

  Integer sum(Kind k, int x, int y) {
    Integer result =
        switch (k) {
          case A -> x + y;
          case B -> x - y;
        };
    // :: error: [assignment]
    @Interned Integer interned = result;
    return result;
  }
}
