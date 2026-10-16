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
}
