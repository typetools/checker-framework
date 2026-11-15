// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that a record pattern in a case other than the last one can throw MatchException.  A record
// pattern calls accessor methods, and the switch wraps any exception that an accessor throws in a
// MatchException (JLS 14.30.2).

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RecordPatternAccessorException {

  sealed interface Shape permits Circle, Square {}

  record Circle(int radius) implements Shape {
    @Override
    public int radius() {
      throw new IllegalStateException();
    }
  }

  record Square(int side) implements Shape {}

  void accessorThrows(Shape s) {
    @Nullable Object x = "";
    try {
      x = null;
      switch (s) {
        case Circle(int r) -> {}
        case Shape t -> {}
      }
      x = "";
    } catch (MatchException e) {
      // :: error: [dereference.of.nullable]
      x.hashCode();
    }
  }

  // A type pattern calls no accessor method.
  void typePattern(Shape s) {
    @Nullable Object x = "";
    try {
      x = null;
      switch (s) {
        case Circle c -> {}
        case Shape t -> {}
      }
      x = "";
    } catch (MatchException e) {
      // The catch block is unreachable.
      x.hashCode();
    }
  }

  // An accessor that has a side effect on a field other than the selector expression.

  static @Nullable Object f;

  record Clearing(int x) implements Shape2 {
    @Override
    public int x() {
      f = null;
      return 0;
    }
  }

  record Throwing(int x) implements Shape2 {
    @Override
    public int x() {
      f = null;
      throw new IllegalStateException();
    }
  }

  sealed interface Shape2 permits Clearing, Throwing {}

  @EnsuresNonNull("RecordPatternAccessorException.f")
  static boolean initialize() {
    f = "";
    return false;
  }

  void accessorClearsField(Clearing c) {
    f = "";
    switch (c) {
      case Clearing(int x) -> {
        // :: error: [dereference.of.nullable]
        f.hashCode();
      }
    }
  }

  void accessorClearsFieldAfterGuard(Clearing c) {
    switch (c) {
      case Clearing whole when initialize() -> {}
      case Clearing(int x) -> {
        // :: error: [dereference.of.nullable]
        f.hashCode();
      }
    }
  }

  void accessorClearsFieldThenThrows(Shape2 s) {
    f = "";
    try {
      switch (s) {
        case Throwing(int x) -> {}
        case Shape2 t -> {}
      }
    } catch (MatchException e) {
      // :: error: [dereference.of.nullable]
      f.hashCode();
    }
  }
}
