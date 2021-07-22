// A simple class that has a Socket as an owning field.
// This test exists to check that we gracefully handle assignments to it.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;
import java.net.*;

@MustCall("close") class SocketContainer2 {

    @Owning Socket sock = new Socket();

    public SocketContainer2(String host, int port) throws Exception {
        // This assignment is safe, because the only possible value of sock here is the unconnected
        // socket in the field initializer.
        sock = new Socket(host, port);
    }

    @EnsuresCalledMethods(value = "this.sock", methods = "close")
    public void close() throws IOException {
        sock.close();
    }
}
