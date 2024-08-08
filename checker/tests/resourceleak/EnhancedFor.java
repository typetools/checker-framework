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

  // not an error anymore with the changed upper bound
  void test3(List<Socket> list) {
    // This is not an error. Even though List<Socket> results in list being
    // List<@MustCall("close") Socket>, s is assigned by a desugared
    // iterator.next(), which is @NonOwning, since it doesn't remove the
    // element from the collection and the collection has the ownership over
    // the elements and the obligation to fulfill their calling requirements.
    for (Socket s : list) {}
  }

  void test4(List<@MustCall Socket> list) {
    for (Socket s : list) {}
  }

  void test5(List<? extends Socket> list) {
    // even though the type variable resolves to @MustCall("close") Socket,
    // this is no error, since s is assigned in a desugared iterator.next(),
    // which is @NonOwning
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
