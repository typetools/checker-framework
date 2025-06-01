// This test case demonstrates that the checker does issue a warning when
// a socket with an obligation is stored into a List<Socket> (which cannot be
// owning).

import java.net.*;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.OwningCollection;
import org.checkerframework.checker.mustcall.qual.*;

public class SocketIntoList {
  public void test1(List<@MustCall({}) Socket> l) {
    // s is unconnected, so no error is expected when it's stored into a list
    Socket s = new Socket();
    l.add(s);
  }

  public void test2(@OwningCollection List<Socket> l) {
    // s is unconnected, so no error is expected when it's stored into the list.
    // But, if the list is unannotated, we do get an error at its declaration site
    // (as expected, due to #5912).
    Socket s = new Socket();
    l.add(s);
  }

  public void test3(List<@MustCall({}) Socket> l) throws Exception {
    // :: error: required.method.not.called
    Socket s = new Socket();
    s.bind(new InetSocketAddress("192.168.0.1", 0));
    l.add(s);
  }

  // This input list might have been produced by e.g., test1()
  public void test4(List<@MustCall({}) Socket> l) throws Exception {
    // :: error: required.method.not.called
    Socket s = l.get(0);
    s.bind(new InetSocketAddress("192.168.0.1", 0));
  }

  // This input list might have been produced by e.g., test1()
  public void test5(List<@MustCall({}) Socket> l) throws Exception {
    // No error is expected here, because the socket should be @MustCall({}) when
    // it is retrieved from the list. (Equivalently, the list is not owning).
    Socket s = l.get(0);
  }
}
