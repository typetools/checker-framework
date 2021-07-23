// a test for missing mustcall propagation that might have caused a false positive?

import org.checkerframework.checker.mustcall.qual.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.*;

class SocketBufferedReader {
    void test(String address, int port) {
        try {
            Socket socket = new Socket(address, 80);
            PrintStream out = new PrintStream(socket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            @MustCall("close") BufferedReader reader = in;
            // :: error: assignment.type.incompatible
            @MustCall({}) BufferedReader reader2 = in;
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
