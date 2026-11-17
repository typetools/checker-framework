// @below-java21-jdk-skip-test

// Test that a case label does not refine a selector expression that a guard might have reassigned.

import org.checkerframework.common.value.qual.EnumVal;
import org.checkerframework.dataflow.qual.Pure;

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

  static E staticField;

  @Pure
  static E getStaticField() {
    return staticField;
  }

  static boolean resetStatic() {
    staticField = E.A;
    return false;
  }

  // A guard might change the result of a method call that is the selector expression.
  void methodCallSelector(E input) {
    staticField = input;
    switch (getStaticField()) {
      case E ignored when resetStatic() -> {}
      case B -> {
        // :: error: (assignment)
        @EnumVal("B") E x = getStaticField();
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
