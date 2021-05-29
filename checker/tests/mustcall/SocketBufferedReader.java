// a test for missing mustcall propagation that might have caused a false positive?

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.*;
import org.checkerframework.checker.mustcall.qual.*;

class SocketBufferedReader {
  void test(String address, int port) {
    try {
      Socket socket = new Socket(address, 80);
      PrintStream out = new PrintStream(socket.getOutputStream());
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      @MustCall("close") BufferedReader reader = in;
      // :: error: assignment
      @MustCall({}) BufferedReader reader2 = in;
      in.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
