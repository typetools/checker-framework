// Test input for ResourceLeakCountMustCallTest.

import java.net.Socket;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;

class ResetOnOneBranch {
  @CreatesMustCallFor("#1")
  static void reset(Socket s) {}

  static void leak(boolean b) {
    // Counted.  Not verified, because it leaks if b is false.
    Socket s = new Socket();
    if (b) {
      // Counted.  Not verified, because it leaks.
      reset(s);
    }
    s.getPort();
  }
}
