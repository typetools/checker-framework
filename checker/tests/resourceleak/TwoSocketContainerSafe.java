// This is the safe version of TwoSocketContainer.java.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import java.net.Socket;

@InheritableMustCall({"close1", "close2"})
public class TwoSocketContainerSafe {
  @Owning
  private final Socket s1, s2;

  public TwoSocketContainerSafe(@Owning Socket s1, @Owning Socket s2) {
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

  public static void test(@Owning Socket sock1, @Owning Socket sock2) throws java.io.IOException {
    TwoSocketContainerSafe tsc = new TwoSocketContainerSafe(sock1, sock2);
    try {
      tsc.close1();
    } catch (Exception io) { }
    finally {
      try {
        tsc.close2();
      } catch (Exception io2) { }
    }
  }
}
