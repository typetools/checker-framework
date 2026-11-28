// Test that a return, break, or continue statement in a finally block leaves the try statement
// without running that finally block again, but still runs the finally blocks of enclosing try
// statements.

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FinallyBlockJumps {

  @Nullable Object f;

  @Nullable Object g;

  @Nullable Object h;

  static @Nullable Object sf;

  @EnsuresNonNull("f")
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

  // The labeled break statement runs the finally block of the enclosing try statement, which sets f
  // to a non-null value.
  @EnsuresNonNull("f")
  void labeledBreakInNestedFinally(boolean b, int n) {
    f = "init";
    outer:
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        try {
          try {
            f = null;
          } finally {
            if (b) {
              break outer;
            }
          }
          f = "c";
        } finally {
          f = "d";
        }
      }
    }
  }

  // The labeled continue statement runs the finally block of the enclosing try statement, which
  // sets f to a non-null value.
  @EnsuresNonNull("f")
  void labeledContinueInNestedFinally(boolean b, int n) {
    f = "init";
    outer:
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        try {
          try {
            f = null;
          } finally {
            if (b) {
              continue outer;
            }
          }
          f = "c";
        } finally {
          f = "d";
        }
      }
    }
  }

  // When the return statement executes, g is null.
  @EnsuresNonNull("g")
  // :: error: (contracts.postcondition)
  void returnInFinallyLeavesNull(boolean b) {
    h = null;
    try {
      f = "a";
    } finally {
      g = h;
      h = "x";
      if (b) {
        return;
      }
    }
    g = "c";
  }

  static class SetsFieldOnClose implements AutoCloseable {
    @Override
    @EnsuresNonNull("FinallyBlockJumps.sf")
    public void close() {
      sf = "closed";
    }
  }

  // The return statement runs the close() method of the enclosing try-with-resources statement,
  // which sets sf to a non-null value.
  @EnsuresNonNull("FinallyBlockJumps.sf")
  void returnInFinallyClosesResource(boolean b) {
    try (SetsFieldOnClose r = new SetsFieldOnClose()) {
      try {
        sf = null;
      } finally {
        if (b) {
          return;
        }
      }
      sf = "c";
    }
  }
}
