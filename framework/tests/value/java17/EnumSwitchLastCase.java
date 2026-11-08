// @below-java17-jdk-skip-test

// Test that the label of the last case of an exhaustive switch refines the selector in the body.

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

  void switchStatement(E e) {
    switch (e) {
      case A -> {}
      case B -> {
        @EnumVal("B") E x = e;
      }
    }
  }
}
