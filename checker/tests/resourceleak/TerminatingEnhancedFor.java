// A test that the Resource Leak Checker honors @TerminatesExecution on a call that an enhanced for
// loop desugars to.

import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import org.checkerframework.dataflow.qual.TerminatesExecution;

class TerminatingEnhancedFor {

  static class TerminatingIterable implements Iterable<String> {
    @Override
    @TerminatesExecution
    public Iterator<String> iterator() {
      throw new Error();
    }
  }

  void m(String host, int port, TerminatingIterable it) throws IOException {
    Socket s = new Socket(host, port);
    for (String x : it) {}
    s.close();
  }

  // The loop's iterator() call terminates execution, so no code after the loop executes, and the
  // socket need not be closed.
  void noCloseAfterLoop(String host, int port, TerminatingIterable it) throws IOException {
    Socket s = new Socket(host, port);
    for (String x : it) {}
  }
}
