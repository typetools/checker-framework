// A test that a terminating call that an enhanced for loop desugars to does not cause a false
// positive.

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
}
