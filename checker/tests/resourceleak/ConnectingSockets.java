// a set of test cases that demonstrate that errors are actually insued in appropriate
// places when Sockets are connected

import java.net.*;
import org.checkerframework.checker.mustcall.qual.*;

class ConnectingSockets {

  static void simple_ns_test(SocketAddress sa) throws Exception {
    // :: error: required.method.not.called
    Socket s = new Socket();
    s.bind(sa);
  }

  static void simple_ns_test2(SocketAddress sa) throws Exception {
    Socket s = new Socket();
    // s.bind(sa);
  }

  static void simple_ns_test3(SocketAddress sa) throws Exception {
    // :: error: required.method.not.called
    Socket s = new Socket();
    s.connect(sa);
  }

  static void simple_ns_test4(SocketAddress sa, int to) throws Exception {
    // :: error: required.method.not.called
    Socket s = new Socket();
    s.connect(sa, to);
  }

  static @MustCall({}) Socket makeUnconnected() throws Exception {
    return new Socket();
  }

  static void simple_ns_test5(SocketAddress sa) throws Exception {
    // :: error: required.method.not.called
    Socket s = makeUnconnected();
    s.bind(sa);
  }

  static void simple_ns_test6(SocketAddress sa) throws Exception {
    Socket s = makeUnconnected();
    // s.bind(sa);
  }

  static void simple_ns_test7(SocketAddress sa) throws Exception {
    // :: error: required.method.not.called
    Socket s = makeUnconnected();
    s.connect(sa);
  }

  static void simple_ns_test8(SocketAddress sa, int to) throws Exception {
    // :: error: required.method.not.called
    Socket s = makeUnconnected();
    s.connect(sa, to);
  }
}
