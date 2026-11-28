// Tests the exceptional copy of a finally block that the CFG builder creates after the extra
// exceptional copies of earlier finally blocks in the same method have used up
// CFGTranslationPhaseOne.EXTRA_EXCEPTIONAL_FINALLY_NODES_BUDGET.  The exceptions that reach such a
// finally block share one exceptional copy of it, which must rethrow each of them to the place
// that it goes:  here, a different catch block for each of IOException, RuntimeException, and
// Error.
//
// The nested finally blocks at the beginning of the method use up the budget.  If the budget is
// increased, deepen their nesting so that they still use it up.

import java.io.IOException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

abstract class FinallyCopyBudget {
  abstract void mightThrow() throws IOException;

  abstract void log();

  void afterBudgetIsUsedUp() {
    // These finally blocks use up the budget.  Each of them has two exceptional copies:  one
    // rethrows IOException to the catch block, and the other rethrows RuntimeException and Error
    // out of the method.  Nesting makes the number of copies grow exponentially.
    try {
      try {
        mightThrow();
      } finally {
        try {
          try {
            mightThrow();
          } finally {
            try {
              try {
                mightThrow();
              } finally {
                try {
                  try {
                    mightThrow();
                  } finally {
                    try {
                      try {
                        mightThrow();
                      } finally {
                        try {
                          try {
                            mightThrow();
                          } finally {
                            try {
                              try {
                                mightThrow();
                              } finally {
                                log();
                              }
                            } catch (IOException e) {
                            }
                          }
                        } catch (IOException e) {
                        }
                      }
                    } catch (IOException e) {
                    }
                  }
                } catch (IOException e) {
                }
              }
            } catch (IOException e) {
            }
          }
        } catch (IOException e) {
        }
      }
    } catch (IOException e) {
    }

    // This finally block has one exceptional copy, which rethrows all three exceptions.  The
    // finally block and the catch blocks contain no method calls, so mightThrow() is the only
    // source of the exceptions that the catch blocks catch.
    @Nullable Object o = "";
    boolean finished = false;
    try {
      try {
        try {
          try {
            o = null;
            mightThrow();
            o = "";
          } finally {
            finished = true;
          }
        } catch (IOException e) {
          // :: error: (assignment)
          @NonNull Object fromIOException = o;
        }
      } catch (RuntimeException e) {
        // :: error: (assignment)
        @NonNull Object fromRuntimeException = o;
      }
    } catch (Error e) {
      // :: error: (assignment)
      @NonNull Object fromError = o;
    }
  }
}
