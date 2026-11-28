// When the Must Call Checker runs on its own, a method or constructor whose collection parameter
// has an unbounded wildcard element type accepts a collection of resources.

import java.net.Socket;
import java.util.List;

class BorrowedCollectionParameter {

  static int count(List<?> list) {
    return list.size();
  }

  BorrowedCollectionParameter(List<?> list) {}

  static void client(List<Socket> sockets) {
    count(sockets);
    new BorrowedCollectionParameter(sockets);
  }
}
