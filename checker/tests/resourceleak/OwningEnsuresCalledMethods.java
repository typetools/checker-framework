// A test that the RLC understands that @Owning on a parameter is effectively
// a stronger version of the @EnsuresCalledMethods annotation

import java.io.*;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("dispose")
public class OwningEnsuresCalledMethods {

  @Owning Socket con;

  @EnsuresCalledMethods(value = "this.con", methods = "close")
  void dispose() {
    closeCon(con);
  }

  static void closeCon(@Owning Socket con) {
    try {
      con.close();
    } catch (IOException e) {
    }
  }
}
