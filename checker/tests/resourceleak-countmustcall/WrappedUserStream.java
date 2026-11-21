// Test input for ResourceLeakCountMustCallTest.

import java.io.BufferedInputStream;
import java.io.InputStream;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;

class WrappedUserStream {
  @InheritableMustCall("close")
  static class UserStream extends InputStream {
    @Override
    public int read() {
      return -1;
    }
  }

  static void leak() {
    // The user stream is not counted, because its class is not in the JDK.  The wrapper is
    // counted, because its class is in the JDK.  It is not verified, because it leaks.
    BufferedInputStream s = new BufferedInputStream(new UserStream());
  }
}
