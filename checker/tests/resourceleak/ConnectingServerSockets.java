// a set of test cases that demonstrate that errors are actually insued in appropriate
// places when ServerSockets are connected

import java.net.*;
import org.checkerframework.checker.mustcall.qual.*;

class ConnectingServerSockets {

  static void simple_ss_test(SocketAddress sa) throws Exception {
    // :: error: required.method.not.called
    ServerSocket s = new ServerSocket();
    s.bind(sa);
  }

  static void simple_ss_test2(SocketAddress sa) throws Exception {
    ServerSocket s = new ServerSocket();
    // s.bind(sa);
  }

  static void simple_ss_test4(SocketAddress sa, int to) throws Exception {
    // :: error: required.method.not.called
    ServerSocket s = new ServerSocket();
    s.bind(sa, to);
  }

  static @MustCall({}) ServerSocket makeUnconnected() throws Exception {
    return new ServerSocket();
  }

  static void simple_ss_test5(SocketAddress sa) throws Exception {
    // :: error: required.method.not.called
    ServerSocket s = makeUnconnected();
    s.bind(sa);
  }

  static void simple_ss_test6(SocketAddress sa) throws Exception {
    ServerSocket s = makeUnconnected();
    // s.bind(sa);
  }

  static void simple_ss_test8(SocketAddress sa, int to) throws Exception {
    // :: error: required.method.not.called
    ServerSocket s = makeUnconnected();
    s.bind(sa, to);
  }
}
