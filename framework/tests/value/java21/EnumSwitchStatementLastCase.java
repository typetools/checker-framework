// @below-java21-jdk-skip-test

// Test that the label of the last case of an exhaustive switch statement refines the selector in
// the body.  A `case null` label makes each switch statement an enhanced switch statement, which
// must be exhaustive.

import org.checkerframework.common.value.qual.EnumVal;

public class EnumSwitchStatementLastCase {

  enum E {
    A,
    B
  }

  void switchStatementArrow(E e) {
    switch (e) {
      case null -> {}
      case A -> {}
      case B -> {
        @EnumVal("B") E x = e;
      }
    }
  }

  void switchStatementColon(E e) {
    switch (e) {
      case null:
        break;
      case A:
        break;
      case B:
        @EnumVal("B") E x = e;
        break;
    }
  }
}
