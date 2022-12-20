// A test that a class with two owned sockets cannot be @MustCallAliased with both of them.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import java.net.Socket;

@InheritableMustCall({"close1", "close2"})
public class TwoSocketContainer {
  @Owning
  private final Socket s1, s2;

  // :: error: mustcallalias.out.of.scope
  public @MustCallAlias TwoSocketContainer(@MustCallAlias Socket s1, @MustCallAlias Socket s2) {
    this.s1 = s1;
    this.s2 = s2;
  }

  @EnsuresCalledMethods(value="this.s1", methods={"close"})
  public void close1() throws java.io.IOException {
    s1.close();
  }

  @EnsuresCalledMethods(value="this.s2", methods={"close"})
  public void close2() throws java.io.IOException {
    s2.close();
  }

  // The following error should be thrown about at least sock2
  // :: error: required.method.not.called
  public static void test(@Owning Socket sock1, @Owning Socket sock2) throws java.io.IOException {
    TwoSocketContainer tsc = new TwoSocketContainer(sock1, sock2);
    sock1.close();
  }
}
