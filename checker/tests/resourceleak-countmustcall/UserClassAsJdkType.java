// Test input for ResourceLeakCountMustCallTest.

import java.io.Closeable;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;

@InheritableMustCall("close")
class UserClassAsJdkType implements Closeable {
  @Override
  public void close() {}

  static void leak() {
    // Not counted, because the type of `new UserClassAsJdkType()` is not in the JDK.  The type of
    // the variable `c`, which is in the JDK, is not considered.
    Closeable c = new UserClassAsJdkType();
  }
}
