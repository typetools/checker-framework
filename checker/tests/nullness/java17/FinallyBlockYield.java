// @below-java17-jdk-skip-test

// Test that a yield statement in a finally block leaves the try statement without running that
// finally block again.

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FinallyBlockYield {

  @Nullable Object f;

  @EnsuresNonNull("f")
  int yieldInFinally(boolean b, int k) {
    return switch (k) {
      case 1 -> {
        try {
          f = "a";
        } finally {
          if (b) {
            yield 3;
          }
          f = null;
        }
        f = "c";
        yield 4;
      }
      default -> {
        f = "d";
        yield 5;
      }
    };
  }

  // When the yield statement executes, f is null.
  @EnsuresNonNull("f")
  // :: error: (contracts.postcondition)
  int yieldInFinallyLeavesNull(boolean b, int k) {
    @Nullable Object local = null;
    int result =
        switch (k) {
          case 1 -> {
            try {
              f = "a";
            } finally {
              f = local;
              local = "x";
              if (b) {
                yield 3;
              }
            }
            f = "c";
            yield 4;
          }
          default -> {
            f = "d";
            yield 5;
          }
        };
    return result;
  }
}
