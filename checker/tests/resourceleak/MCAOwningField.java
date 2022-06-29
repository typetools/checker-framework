// A test case for a common pattern in Zookeeper: something is must-call-alias
// with an owning field, and therefore a false positive was issued.

import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("stop") class MCAOwningField {

  @Owning final Socket s;

  MCAOwningField() throws Exception {
    s = new Socket();
  }

  void simple() throws Exception {
    s.getInputStream();
  }

  @EnsuresCalledMethods(value = "s", methods = "close")
  void stop() throws Exception {
    s.close();
  }
}
