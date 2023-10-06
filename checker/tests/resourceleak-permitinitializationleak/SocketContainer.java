// A simple class that has a Socket as an owning field.
// This test exists to check that we gracefully handle assignments 1)
// in the constructor and 2) to null.

import java.io.*;
import java.net.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("close")
class SocketContainer {
  @Owning Socket sock;

  public SocketContainer(String host, int port) throws Exception {
    sock = new Socket(host, port);
    try {
      sock = new Socket(host, port);
    } catch (Exception ignored) {
    }
  }

  @EnsuresCalledMethods(value = "this.sock", methods = "close")
  public void close() throws IOException {
    sock.close();
    // It's okay to assign a field to null after its obligations have been fulfilled,
    // without inducing a reset.
    sock = null;
  }
}
