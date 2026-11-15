// Test input for ResourceLeakCountMustCallTest.

import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;

class ResetOnOneBranch {
  @CreatesMustCallFor("#1")
  static void reset(Socket s) {}

  static void leak(boolean b, String host, int port) throws IOException {
    // Counted.  Not verified, because it leaks if b is false.  (An unconnected socket, as created
    // by `new Socket()`, has no must-call obligation and is not counted.)
    Socket s = new Socket(host, port);
    if (b) {
      // Counted.  Not verified, because it leaks.
      reset(s);
    }
    s.getPort();
  }
}
