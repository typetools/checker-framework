// Test input for ResourceLeakCountMustCallTest.

import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;

class ResetThrows {
  @CreatesMustCallFor("#1")
  static void reset(Socket s) throws IOException {}

  static void leak() throws IOException {
    // Counted.  Not verified, because it leaks if reset() throws an exception.
    Socket s = new Socket();
    // Counted.  Not verified, because it leaks.
    reset(s);
  }
}
