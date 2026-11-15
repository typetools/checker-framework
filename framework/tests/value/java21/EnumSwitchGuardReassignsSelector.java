// @below-java21-jdk-skip-test

// Test that a case label does not refine a selector expression that a guard might have reassigned.

import org.checkerframework.common.value.qual.EnumVal;

public class EnumSwitchGuardReassignsSelector {

  enum E {
    A,
    B
  }

  E field;

  boolean reset() {
    field = E.A;
    return false;
  }

  void lastCase(E input) {
    field = input;
    switch (field) {
      case E ignored when reset() -> {}
      case A -> {}
      case B -> {
        // :: error: (assignment)
        @EnumVal("B") E x = field;
      }
    }
  }

  void notLastCase(E input) {
    field = input;
    switch (field) {
      case E ignored when reset() -> {}
      case B -> {
        // :: error: (assignment)
        @EnumVal("B") E x = field;
      }
      case A -> {}
    }
  }

  void noGuard(E input) {
    field = input;
    switch (field) {
      case A -> {}
      case B -> {
        @EnumVal("B") E x = field;
      }
    }
  }

  void localSelector(E input) {
    E local = input;
    switch (local) {
      case E ignored when reset() -> {}
      case A -> {}
      case B -> {
        @EnumVal("B") E x = local;
      }
    }
  }
}
