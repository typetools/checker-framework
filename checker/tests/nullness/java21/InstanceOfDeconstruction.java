// @below-java21-jdk-skip-test

// Test that the binding variables of a deconstruction pattern have the types of the record
// components.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class InstanceOfDeconstruction {

  record Point(String name, int y) {}

  record NPoint(@Nullable String name, int y) {}

  record Box<T>(T t) {}

  void nonNullComponent(Object o) {
    if (o instanceof Point(String n, int y)) {
      n.length();
    }
  }

  void nullableComponent(Object o) {
    if (o instanceof NPoint(String n, int y)) {
      // :: error: [dereference.of.nullable]
      n.length();
    }
  }

  void nullableComponentVar(Object o) {
    if (o instanceof NPoint(var n, var y)) {
      // :: error: [dereference.of.nullable]
      n.length();
    }
  }

  int nullableComponentSwitch(Object o) {
    return switch (o) {
      case NPoint(String n, int y) ->
          // :: error: [dereference.of.nullable]
          n.length();
      case Point(String n, int y) -> n.length();
      default -> 0;
    };
  }

  void explicitlyAnnotated(Object o) {
    // :: warning: [instanceof.pattern.unsafe]
    if (o instanceof NPoint(@NonNull String n, int y)) {
      n.length();
    }
  }

  record NBox(@Nullable Object o) {}

  void typeTest(Object o) {
    // A nested type pattern that is not unconditional does not match null.
    if (o instanceof NBox(String s)) {
      s.length();
    }
    if (o instanceof NBox(Object x)) {
      // :: error: [dereference.of.nullable]
      x.toString();
    }
  }

  void genericComponent(Box<@Nullable String> b) {
    if (b instanceof Box(String s)) {
      // TODO: Issue a dereference.of.nullable error.  A component whose type is a type variable
      // is not handled.
      s.length();
    }
  }
}
