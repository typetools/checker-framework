// Test that a return, break, or continue statement in a finally block leaves the try statement
// without running that finally block again, but still runs the finally blocks of enclosing try
// statements.

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

  @EnsuresNonNull("f")
  @SuppressWarnings("finally") // a finally block that does not complete normally
  void unlabeledBreakInFinally(boolean b, int n) {
    f = "init";
    for (int i = 0; i < n; i++) {
      try {
        f = "a";
      } finally {
        if (b) {
          break;
        }
        f = null;
      }
      f = "c";
    }
  }

  @EnsuresNonNull("f")
  @SuppressWarnings("finally") // a finally block that does not complete normally
  void continueInFinally(boolean b, int n) {
    f = "init";
    for (int i = 0; i < n; i++) {
      try {
        f = "a";
      } finally {
        if (b) {
          continue;
        }
        f = null;
      }
      f = "c";
    }
  }

  @EnsuresNonNull("f")
  @SuppressWarnings("finally") // a finally block that does not complete normally
  void labeledContinueInFinally(boolean b, int n) {
    f = "init";
    outer:
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        try {
          f = "a";
        } finally {
          if (b) {
            continue outer;
          }
          f = null;
        }
        f = "c";
      }
    }
  }

  // The return statement runs the finally block of the enclosing try statement, which sets f to a
  // non-null value.
  @EnsuresNonNull("f")
  @SuppressWarnings("finally") // a finally block that does not complete normally
  void returnInNestedFinallyEstablishesPostcondition(boolean b) {
    try {
      try {
        f = null;
      } finally {
        if (b) {
          return;
        }
      }
      f = "c";
    } finally {
      f = "d";
    }
  }

  // The break statement runs the finally block of the enclosing try statement, which sets f to a
  // non-null value.
  @EnsuresNonNull("f")
  @SuppressWarnings("finally") // a finally block that does not complete normally
  void breakInNestedFinally(boolean b, int n) {
    f = "init";
    for (int i = 0; i < n; i++) {
      try {
        try {
          f = null;
        } finally {
          if (b) {
            break;
          }
        }
        f = "c";
      } finally {
        f = "d";
      }
    }
  }

  // The continue statement runs the finally block of the enclosing try statement, which sets f to
  // a non-null value.
  @EnsuresNonNull("f")
  @SuppressWarnings("finally") // a finally block that does not complete normally
  void continueInNestedFinally(boolean b, int n) {
    f = "init";
    for (int i = 0; i < n; i++) {
      try {
        try {
          f = null;
        } finally {
          if (b) {
            continue;
          }
        }
        f = "c";
      } finally {
        f = "d";
      }
    }
  }
}
