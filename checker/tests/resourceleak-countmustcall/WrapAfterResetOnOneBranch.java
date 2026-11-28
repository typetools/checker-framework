// Test input for ResourceLeakCountMustCallTest.

import java.io.BufferedInputStream;
import java.io.InputStream;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;

class WrapAfterResetOnOneBranch {
  @InheritableMustCall("close")
  static class UserStream extends InputStream {
    @Override
    public int read() {
      return -1;
    }
  }

  @CreatesMustCallFor("#1")
  static native void reset(InputStream s);

  static void leak(boolean b) {
    // Not counted, because its class is not in the JDK.
    InputStream s = new UserStream();
    if (b) {
      // Counted, because the type of s is in the JDK.  Not verified, because it leaks.
      reset(s);
    }
    // Counted, because the stream that it wraps is not counted if b is false.  Not verified,
    // because it leaks.
    BufferedInputStream w = new BufferedInputStream(s);
  }
}
