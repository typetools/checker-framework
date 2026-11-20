// `throw null` throws a NullPointerException, which the Resource Leak Checker ignores, as it does
// for `throw new NullPointerException()`.

import java.io.IOException;
import java.net.Socket;

public class ThrowNull {

  void throwNull(String host, int port) throws IOException {
    Socket s = new Socket(host, port);
    throw null;
  }

  void throwNullPointerException(String host, int port) throws IOException {
    Socket s = new Socket(host, port);
    throw new NullPointerException();
  }

  void throwIllegalStateException(String host, int port) throws IOException {
    // :: error: [required.method.not.called]
    Socket s = new Socket(host, port);
    throw new IllegalStateException();
  }

  void throwNullInTry(String host, int port) throws IOException {
    Socket s = new Socket(host, port);
    try {
      throw null;
    } catch (NullPointerException e) {
      s.close();
    }
  }
}
