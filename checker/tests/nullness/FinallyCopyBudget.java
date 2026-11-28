// Tests the exceptional copy of a finally block that the CFG builder creates after the extra
// exceptional copies of earlier finally blocks in the same method have used up
// CFGTranslationPhaseOne.EXTRA_EXCEPTIONAL_FINALLY_NODES_BUDGET.  The exceptions that reach such a
// finally block share one exceptional copy of it, which must rethrow each of them to the place
// that it goes:  here, a different catch block for each of IOException, RuntimeException, and
// Error.  This test checks that each catch block is reachable; method
// finallyCopyAfterBudgetIsUsedUp in dataflow/tests/cfgconstruction/Test.java checks that the
// exceptional copy is shared.
//
// The nested finally blocks at the beginning of afterBudgetIsUsedUp() use up the budget.  If they
// do not, the last finally block gets a separate copy for IOException, and the expected error in
// its catch block for IOException does not occur, so the test fails.  In that case, deepen their
// nesting so that they use up the budget.

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

    // This finally block has one exceptional copy, which rethrows all three exceptions.  An
    // IOException comes only from mightThrow(), when o is non-null.  A RuntimeException or an Error
    // also comes from log(), when o is null.  The copy merges the stores of all those paths, so o
    // might be null in each catch block, even the one for IOException.  The finally block and the
    // catch blocks contain no method calls, so mightThrow() and log() are the only sources of the
    // exceptions that the catch blocks catch.
    @Nullable Object o = "";
    boolean finished = false;
    try {
      try {
        try {
          try {
            mightThrow();
            o = null;
            log();
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

  void beforeBudgetIsUsedUp() {
    // The same code as at the end of afterBudgetIsUsedUp(), but no earlier finally block uses up
    // the budget.  So IOException has its own copy of the finally block, in which o is non-null.
    @Nullable Object o = "";
    boolean finished = false;
    try {
      try {
        try {
          try {
            mightThrow();
            o = null;
            log();
            o = "";
          } finally {
            finished = true;
          }
        } catch (IOException e) {
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
