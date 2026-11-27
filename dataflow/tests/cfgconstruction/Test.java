import java.io.IOException;

public class Test {

  public void manyNestedTryFinallyBlocks() {
    try {
      System.out.println("!");
    } finally {
      try {
        System.out.println("!");
      } finally {
        try {
          System.out.println("!");
        } finally {
          try {
            System.out.println("!");
          } finally {
            try {
              System.out.println("!");
            } finally {
              System.out.println("!");
            }
          }
        }
      }
    }
  }

  public Runnable lambdaAndClassInFinallyBlock(boolean b) {
    try {
      if (b) {
        return null;
      }
      System.out.println("!");
    } finally {
      Runnable r = () -> System.out.println("!");
      class Local {}
      r.run();
    }
    return null;
  }

  void mightThrow() throws IOException {}

  void log() {}

  // The finally block that assigns `finished` has one exceptional copy, which rethrows
  // IOException, RuntimeException, and Error to three different catch blocks.  The nested finally
  // blocks before it use up CFGTranslationPhaseOne.EXTRA_EXCEPTIONAL_FINALLY_NODES_BUDGET.  If the
  // budget is increased, deepen their nesting so that they still use it up.  This method is like
  // the method in checker/tests/nullness/FinallyCopyBudget.java.
  public void finallyCopyAfterBudgetIsUsedUp() {
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

    boolean finished = false;
    try {
      try {
        try {
          try {
            mightThrow();
          } finally {
            finished = true;
          }
        } catch (IOException e) {
        }
      } catch (RuntimeException e) {
      }
    } catch (Error e) {
    }
  }

  // This method is like the end of the previous one.  Because the budget is not used up, the
  // finally block that assigns `finished` has three exceptional copies, one for each of
  // IOException, RuntimeException, and Error.
  public void finallyCopiesBeforeBudgetIsUsedUp() {
    boolean finished = false;
    try {
      try {
        try {
          try {
            mightThrow();
          } finally {
            finished = true;
          }
        } catch (IOException e) {
        }
      } catch (RuntimeException e) {
      }
    } catch (Error e) {
    }
  }
}
