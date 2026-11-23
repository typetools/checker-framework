// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that a nested type pattern whose type is a non-leftmost bound of a type variable does not
// match a null record component, so a later case is reachable.

import org.checkerframework.checker.nullness.qual.Nullable;

public class NestedPatternNullComponent {

  static class Base {}

  interface Marker {}

  record Box<X>(X x) {}

  <T extends Base & Marker> void nonLeftmostBound(Box<T> box) {
    @Nullable Object o = null;
    switch (box) {
      case Box<T>(Marker m) -> o = "";
      // Reached for `new Box<T>(null)`.
      default -> {}
    }
    // :: error: [dereference.of.nullable]
    o.hashCode();
  }

  <T extends Base & Marker> void leftmostBound(Box<T> box) {
    @Nullable Object o = null;
    switch (box) {
      case Box<T>(Base b) -> o = "";
      // Unreachable:  `Base b` matches every component, including null.
      default -> {}
    }
    o.hashCode();
  }
}
