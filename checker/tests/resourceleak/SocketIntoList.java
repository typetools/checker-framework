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

  public List<Socket> test2(@OwningCollection List<Socket> l) {
    // s is unconnected, so no error is expected when it's stored into the list.
    // However, l would be allowed to store even connected sockets.
    Socket s = new Socket();
    l.add(s);
    return l;
  }

  public void test3(List<@MustCall({}) Socket> l) throws Exception {
    // although s is illegally assigned into the list l, an error
    // for required.method.not.called is not additionally reported at
    // this declaration site of s. list#add(@Owning E) takes on
    // the obligation of the argument.
    Socket s = new Socket();
    s.bind(new InetSocketAddress("192.168.0.1", 0));
    // l cannot hold elements with non-empty @MustCall values
    // :: error: argument
    l.add(s);
  }

  // This input list might have been produced by e.g., test1()
  public void test4(List<@MustCall({}) Socket> l) throws Exception {
    // l.get(0) is not an error as List#get returns @NotOwning.
    // However, s.bind tries to reset the mustcall obligations of s,
    // which is only permitted if s is owning.
    Socket s = l.get(0);
    // :: error: reset.not.owning
    s.bind(new InetSocketAddress("192.168.0.1", 0));
  }

  // This input list might have been produced by e.g., test1()
  public void test5(List<@MustCall({}) Socket> l) throws Exception {
    // No error is expected here, because the socket should be @MustCall({}) when
    // it is retrieved from the list. (Equivalently, the list is not owning).
    Socket s = l.get(0);
  }
}
