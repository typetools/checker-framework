// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that matching a record pattern applies all side effects of an impure record accessor, not
// only its effect on the switch selector expression.

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RecordAccessorSideEffects {

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

  @EnsuresNonNull("RecordAccessorSideEffects.f")
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

  // The last case of an exhaustive switch throws MatchException if an accessor throws an exception.
  void accessorClearsFieldThenThrowsLastCase(Shape2 s) {
    f = "";
    try {
      switch (s) {
        case Clearing c -> {}
        case Throwing(int x) -> {}
      }
    } catch (MatchException e) {
      // :: error: [dereference.of.nullable]
      f.hashCode();
    }
  }
}
