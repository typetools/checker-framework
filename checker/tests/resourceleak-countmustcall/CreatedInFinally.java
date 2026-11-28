// Test input for ResourceLeakCountMustCallTest.

import java.io.FileInputStream;
import java.io.IOException;

class CreatedInFinally {
  static void use() throws IOException {
    try {
      System.out.println("try");
    } finally {
      // Counted once, though the CFG contains a copy of the finally block for each way that the
      // try block exits.  Verified.
      FileInputStream s = new FileInputStream("a");
      s.close();
    }
  }
}
