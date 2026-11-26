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
}
