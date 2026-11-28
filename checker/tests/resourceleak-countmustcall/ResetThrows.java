// Test input for ResourceLeakCountMustCallTest.

import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;

class ResetThrows {
  @CreatesMustCallFor("#1")
  static void reset(Socket s) throws IOException {}

  static void leak(String host, int port) throws IOException {
    // Counted.  Not verified, because it leaks if reset() throws an exception.  (An unconnected
    // socket, as created by `new Socket()`, has no must-call obligation and is not counted.)
    Socket s = new Socket(host, port);
    // Counted.  Not verified, because it leaks.
    reset(s);
  }
}
