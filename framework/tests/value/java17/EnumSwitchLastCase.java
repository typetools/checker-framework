// @below-java17-jdk-skip-test

// Test that the label of the last case of an exhaustive switch expression refines the selector in
// the body.  EnumSwitchStatementLastCase.java tests a switch statement.

import org.checkerframework.common.value.qual.EnumVal;

public class EnumSwitchLastCase {

  enum E {
    A,
    B
  }

  int switchExpression(E e) {
    return switch (e) {
      case A -> 1;
      case B -> {
        @EnumVal("B") E x = e;
        yield 2;
      }
    };
  }
}
