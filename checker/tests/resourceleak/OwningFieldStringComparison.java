// Test case for

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import java.net.Socket;

@InheritableMustCall("a")
public class OwningFieldStringComparison {

  // :: error: required.method.not.called
  @Owning Socket s;

  // important to the bug: the name of this field must contain
  // the name of the owning socket
  /* @NotOwning */ Socket s2;

  // Note this "destructor" closes the wrong socket
  @EnsuresCalledMethods(value="this.s2", methods="close")
  public void a() {
    try {
      this.s2.close();
    } catch(Exception e) {

    } finally {
      try {
        this.s2.close();
      } catch (Exception e) {

      }
    }
  }
}
