import org.checkerframework.checker.mustcall.qual.MustCall;
import java.net.Socket;

class SimpleSocketField {
  Socket mySock = new Socket();

  SimpleSocketField() throws Exception {
    @MustCall("close") Socket s = mySock;
    // :: error: assignment
    @MustCall({}) Socket s1 = mySock;
  }

  void test() {
    @MustCall("close") Socket s = mySock;
    // :: error: assignment
    @MustCall({}) Socket s1 = mySock;
  }
}
