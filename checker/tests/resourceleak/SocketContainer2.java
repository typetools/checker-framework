// A simple class that has a Socket as an owning field.
// This test exists to check that we gracefully handle assignments to it.

import java.io.*;
import java.net.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("close") class SocketContainer2 {

  @Owning Socket sock = new Socket();

  public SocketContainer2(String host, int port) throws Exception {
    // It's not okay to assign to a field with an initializer!
    // :: error: required.method.not.called
    sock = new Socket(host, port);
  }

  @EnsuresCalledMethods(value = "this.sock", methods = "close")
  public void close() throws IOException {
    sock.close();
  }
}
