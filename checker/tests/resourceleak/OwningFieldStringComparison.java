// Test case for https://github.com/typetools/checker-framework/issues/6276

import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("a")
public class OwningFieldStringComparison {

  // :: error: required.method.not.called
  @Owning Socket s;

  // important to the bug: the name of this field must contain
  // the name of the owning socket
  /* @NotOwning */ Socket s2;

  // Note this "destructor" closes the wrong socket
  @EnsuresCalledMethods(value = "this.s2", methods = "close")
  public void a() {
    try {
      this.s2.close();
    } catch (Exception e) {

    } finally {
      try {
        this.s2.close();
      } catch (Exception e) {

      }
    }
  }
}
