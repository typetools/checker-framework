// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// A case does not refine the switch selector expression if the guard of an earlier case might have
// changed it.
public class SwitchSelectorChangedByGuard {

  enum E {
    A,
    B
  }

  void changedByGuard(@Nullable Object x, @Nullable Object y) {
    if (x == null) {
      return;
    }
    switch (x) {
      case Integer i when i > ((x = y) == null ? 0 : 1) -> {}
      case E.A -> {
        // :: error: [assignment]
        @NonNull Object z = x;
      }
      default -> {}
    }
  }

  void changedByGuardCast(@Nullable Object x, @Nullable Object y) {
    if (x == null) {
      return;
    }
    switch ((Object) x) {
      case Integer i when i > ((x = y) == null ? 0 : 1) -> {}
      case E.A -> {
        // :: error: [assignment]
        @NonNull Object z = x;
      }
      default -> {}
    }
  }

  void unchangedByGuard(@Nullable Object x, @Nullable Object y) {
    if (x == null) {
      return;
    }
    switch ((Object) x) {
      case Integer i when i > 0 -> {}
      case E.A -> {
        @NonNull Object z = x;
      }
      default -> {}
    }
  }
}
