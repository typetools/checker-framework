// A simple class that has a Socket as an owning field.
// This test exists to check that we gracefully handle assignments.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;
import java.net.*;

@MustCall("close") class SocketContainer3 {
    @Owning Socket sock = null;

    public SocketContainer3(String host, int port) throws Exception {
        sock = new Socket(host, port);
    }

    @EnsuresCalledMethods(value = "this.sock", methods = "close")
    public void close() throws IOException {
        sock.close();
    }
}
