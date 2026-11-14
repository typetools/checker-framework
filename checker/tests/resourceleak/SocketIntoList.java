// This test case demonstrates that the checker does issue a warning when
// a socket with an obligation is stored into a List<Socket> (which cannot be
// owning).

import java.net.*;
import java.util.List;
import org.checkerframework.checker.mustcall.qual.*;

public class SocketIntoList {
  public void test1(List<@MustCall({}) Socket> l) {
    // s is unconnected, so no error is expected when it's stored into a list
    Socket s = new Socket();
    l.add(s);
  }

  public void test2(List<Socket> l) {
    // s is unconnected, so it has no obligation. Before #7602, the unannotated list was a
    // type.argument error (#5912). Now l is a @NotOwningCollection parameter, which accepts
    // only values that are definitely not owning.
    Socket s = new Socket();
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    l.add(s);
  }

  // l is not owning collection, so the ownership of s is not transferred to l. So we get the
  // required.method.not.called error for s.
  public void test3(List<@MustCall({}) Socket> l) throws Exception {
    // :: error: required.method.not.called
    Socket s = new Socket();
    s.bind(new InetSocketAddress("192.168.0.1", 0));
    l.add(s);
  }

  // This input list might have been produced by e.g., test1()
  public void test4(List<@MustCall({}) Socket> l) throws Exception {
    // Before #7602, s was owning and never closed (required.method.not.called). Now List#get
    // returns @NotOwning, so s is not owning even though it is a local variable, and s.bind
    // cannot reset its obligations.
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
