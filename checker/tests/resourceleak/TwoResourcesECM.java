// A test that shows that no unsoundess occurs when a single close() method is responsible
// for closing two resources.

import java.net.Socket;
import java.io.IOException;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("dispose")
class TwoResourcesECM {
  @Owning Socket s1, s2;

  @EnsuresCalledMethods(value = {"this.s1", "this.s2"}, methods="close")
  public void dispose() throws IOException {
    s1.close();
    s2.close();
  }

  // :: error: required.method.not.called
  static void test1(TwoResourcesECM obj) {
    try {
      obj.dispose();
    } catch (IOException ioe) {

    }
  }
}
