// A test that @Owning on a parameter does not imply @EnsuresCalledMethods.
//
// This was originally:
//
// A test that the RLC understands that @Owning on a parameter is effectively
// a stronger version of the @EnsuresCalledMethods annotation
//
// However, that behavior has since been changed.  An @Owning parameter can be
// satisfied by assigning the value to an @Owning field, in which case the
// methods have not been called.  In other words, @Owning promises to call the
// must-call methods at some point in the future through a different alias; it
// does not promise to call those methods before returning.

import java.io.*;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("dispose")
public class OwningEnsuresCalledMethods {

  @Owning Socket con;

  @EnsuresCalledMethods(value = "this.con", methods = "close")
  // ::error: (contracts.postcondition)
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
