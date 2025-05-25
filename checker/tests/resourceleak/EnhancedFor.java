// Based on some false positives I found in ZK.

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import org.checkerframework.checker.mustcall.qual.*;

class EnhancedFor {
  void test(List<@MustCall Socket> list) {
    for (Socket s : list) {
      try {
        s.close();
      } catch (IOException i) {
      }
    }
  }

  void test2(List<@MustCall Socket> list) {
    for (int i = 0; i < list.size(); i++) {
      Socket s = list.get(i);
      try {
        s.close();
      } catch (IOException io) {
      }
    }
  }

  void test3(List<Socket> list) {
    // This error is issued because `s` is a local variable, and
    // the foreach loop under the hood assigns the result of a call
    // to Iterator#next into it (which is owning by default, because it's
    // a method return type). Both this error and the type.argument error
    // above can be suppressed by writing @MustCall on the Socket type, as in
    // test4 below (but note that this will make call sites difficult to verify).
    // :: error: (required.method.not.called)
    for (Socket s : list) {}
  }

  void test4(List<@MustCall Socket> list) {
    for (Socket s : list) {}
  }

  void test5(List<? extends Socket> list) {
    for (Socket s : list) {}
  }

  void test6(List<? extends Socket> list) {
    for (Socket s : list) {
      try {
        s.close();
      } catch (IOException i) {
      }
    }
  }

  void test7(List<? extends Socket> list) {
    for (int i = 0; i < list.size(); i++) {
      Socket s = list.get(i);
      try {
        s.close();
      } catch (IOException io) {
      }
    }
  }
}
