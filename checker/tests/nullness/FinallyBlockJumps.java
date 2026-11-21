// Test that a return or break statement in a finally block leaves the try statement directly,
// without running the finally block again.

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FinallyBlockJumps {

  @Nullable Object f;

  @EnsuresNonNull("f")
  @SuppressWarnings("finally") // a finally block that does not complete normally
  void returnInFinally(boolean b) {
    try {
      f = "a";
    } finally {
      if (b) {
        return;
      }
      f = null;
    }
    f = "c";
  }

  @EnsuresNonNull("f")
  @SuppressWarnings("finally") // a finally block that does not complete normally
  void breakInFinally(boolean b) {
    label:
    {
      try {
        f = "a";
      } finally {
        if (b) {
          break label;
        }
        f = null;
      }
      f = "c";
    }
  }

  @EnsuresNonNull("f")
  @SuppressWarnings("finally") // a finally block that does not complete normally
  void returnInFinallyAfterException(boolean b, int divisor) {
    try {
      f = "a";
      int quotient = 1 / divisor; // throws ArithmeticException if divisor is 0
    } finally {
      if (b) {
        return;
      }
      f = null;
    }
    f = "c";
  }
}
