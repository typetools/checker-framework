// A test case for an interaction between CO and aliasing that could
// lead to a soundness bug if handled wrong.

import java.net.*;

class UnconnectedSocketAlias {
  void test(SocketAddress sa) throws Exception {
    // :: error: required.method.not.called
    Socket s = new Socket();
    Socket t = s;
    t.close();
    s.connect(sa);
  }
}
