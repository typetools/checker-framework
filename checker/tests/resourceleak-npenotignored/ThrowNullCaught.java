// `throw null` throws a NullPointerException, which a handler for NullPointerException catches.
// This test runs with an ignored-exceptions list that omits NullPointerException, so the Resource
// Leak Checker analyzes the edge from `throw null` to the handler.

import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsOnException;

public abstract class ThrowNullCaught {

  // Closes the socket without dereferencing it, which would add an exceptional edge for a
  // NullPointerException.
  @EnsuresCalledMethods(value = "#1", methods = "close")
  @EnsuresCalledMethodsOnException(value = "#1", methods = "close")
  abstract void closeSocket(Socket s);

  void closeInHandler(String host, int port) throws IOException {
    Socket s = new Socket(host, port);
    try {
      throw null;
    } catch (NullPointerException e) {
      closeSocket(s);
    }
  }

  void noCloseInHandler(String host, int port) throws IOException {
    // :: error: [required.method.not.called]
    Socket s = new Socket(host, port);
    try {
      throw null;
    } catch (NullPointerException e) {
    }
  }

  void throwNull(String host, int port) throws IOException {
    // :: error: [required.method.not.called]
    Socket s = new Socket(host, port);
    throw null;
  }
}
