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

  class Mutator {
    @Override
    public String toString() {
      field = E.A;
      return "";
    }
  }

  // String concatenation calls toString(), which might reassign the selector expression.
  void stringConcatenationGuard(E input, Mutator mutator) {
    field = input;
    switch (field) {
      case E ignored when ("" + mutator) == null -> {}
      case B -> {
        // :: error: (assignment)
        @EnumVal("B") E x = field;
      }
      case A -> {}
    }
  }

  // String conversion of a primitive, boxed primitive, or String has no side effect.
  void stringConcatenationNoSideEffects(E input, int i, Integer boxed, String s) {
    field = input;
    switch (field) {
      case E ignored when ("" + i + boxed + s) == null -> {}
      case B -> {
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

  void stringConcatenationGuardLastCase(E input, Mutator mutator) {
    field = input;
    switch (field) {
      // String concatenation calls mutator.toString(), which reassigns the selector.
      case E ignored when ("" + mutator) == null -> {}
      case A -> {}
      case B -> {
        // :: error: (assignment)
        @EnumVal("B") E x = field;
      }
    }
  }

  @org.checkerframework.dataflow.qual.Pure
  static E current() {
    return staticField;
  }

  void staticGetterSelector() {
    switch (current()) {
      case E ignored when resetStatic() -> {}
      case A -> {}
      case B -> {
        // :: error: (assignment)
        @EnumVal("B") E x = current();
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
