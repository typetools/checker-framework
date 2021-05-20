// A simple test for the @EnsuresCalledMethodsVarArgs annotation.

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import org.checkerframework.checker.calledmethods.qual.*;

class EnsuresCalledMethodsVarArgsSimple {

  // :: error: ensuresvarargs.unverified
  @EnsuresCalledMethodsVarArgs("close")
  void closeAll(Socket... sockets) {
    for (Socket s : sockets) {
      try {
        s.close();
      } catch (IOException e) {
      }
    }
  }

  @EnsuresCalledMethodsVarArgs("close")
  // :: error: ensuresvarargs.invalid
  void closeAllNotVA(List<Socket> sockets) {
    for (Socket s : sockets) {
      try {
        s.close();
      } catch (IOException e) {
      }
    }
  }

  void test(Socket s1, Socket s2) {
    closeAll(s1, s2);
    @CalledMethods("close") Socket s1_1 = s1;
    @CalledMethods("close") Socket s2_1 = s2;
  }
}
