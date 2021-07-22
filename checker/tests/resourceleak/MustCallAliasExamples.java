// Simple tests of @MustCallAlias functionality on wrapper streams.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;
import java.io.IOException;
import java.net.*;

class MustCallAliasExamples {

    void test_two_locals(String address) {
        Socket socket = null;
        try {
            socket = new Socket(address, 8000);
            DataInputStream d = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {

        } finally {
            closeSocket(socket);
        }
    }

    void test_close_wrapper(@Owning InputStream b) throws IOException {
        DataInputStream d = new DataInputStream(b);
        d.close();
    }

    void test_close_nonwrapper(@Owning InputStream b) throws IOException {
        DataInputStream d = new DataInputStream(b);
        b.close();
    }

    // :: error: required.method.not.called
    void test_no_close(@Owning InputStream b) {
        DataInputStream d = new DataInputStream(b);
    }

    // :: error: required.method.not.called
    void test_no_assign(@Owning InputStream b) {
        new DataInputStream(new BufferedInputStream(b));
    }

    @EnsuresCalledMethods(value = "#1", methods = "close")
    void closeSocket(Socket sock) {
        try {
            if (sock != null) {
                sock.close();
            }
        } catch (IOException e) {

        }
    }
}
