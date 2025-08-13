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
    // With the extension of the RLC to collections, this is no longer an error.
    // The return type of Iterator#next is NotOwning, because the iterator does not
    // own the elements, but instead the host collection the iterator is associated with.
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
