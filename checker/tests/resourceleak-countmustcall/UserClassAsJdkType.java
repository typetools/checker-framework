// Test input for ResourceLeakCountMustCallTest.

import java.io.Closeable;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;

@InheritableMustCall("close")
class UserClassAsJdkType implements Closeable {
  @Override
  public void close() {}

  static void leak() {
    // The object's class is not in the JDK, so it is not counted, even though the variable's type
    // is in the JDK.
    Closeable c = new UserClassAsJdkType();
  }
}
