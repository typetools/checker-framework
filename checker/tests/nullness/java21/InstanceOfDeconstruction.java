// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that the binding variables of a deconstruction pattern have the types of the record
// components.

import org.checkerframework.checker.nullness.qual.Nullable;

public class InstanceOfDeconstruction {

  record Point(String name, int y) {}

  record NPoint(@Nullable String name, int y) {}

  record Box<T>(T t) {}

  record NBox(@Nullable Object o) {}

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

  interface Container<T> {}

  record CBox<T>(T value) implements Container<T> {}

  void genericComponentFromSuperinterface(
      Container<String> c, Container<@Nullable String> nc, Container<?> wc) {
    if (c instanceof CBox(var v)) {
      v.length();
    }
    if (c instanceof CBox(String v)) {
      v.length();
    }
    if (nc instanceof CBox(var v)) {
      // :: error: [dereference.of.nullable]
      v.length();
    }
    if (wc instanceof CBox(var v)) {
      // :: error: [dereference.of.nullable]
      v.toString();
    }
  }

  void genericComponentFromObject(Object o) {
    if (o instanceof Box(Object x)) {
      // :: error: [dereference.of.nullable]
      x.toString();
    }
  }

  void nested(Object o) {
    if (o instanceof Box(NPoint(String n, int y))) {
      // :: error: [dereference.of.nullable]
      n.length();
    }
  }

  sealed interface Shape permits Circle, Square {}

  record Circle(@Nullable String label) implements Shape {}

  record Square(@Nullable String label) implements Shape {}

  sealed interface I permits A, B {}

  final class A implements I {}

  final class B implements I {
    void foo() {}
  }

  record IBox(@Nullable I i) {}

  int lastCaseOfExhaustiveSwitch(Shape s) {
    return switch (s) {
      case Circle(String label) -> 0;
      case Square(String label) ->
          // :: error: [dereference.of.nullable]
          label.length();
    };
  }

  void lastCaseTypeTest(IBox ib) {
    switch (ib) {
      case IBox(A a) -> {}
      case IBox(B b) -> b.foo();
    }
  }
}
