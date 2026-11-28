// Test input for ResourceLeakCountMustCallTest.

import java.net.Socket;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;

class ResetEnclosingTarget {
  @CreatesMustCallFor("#1")
  static native void reset(Socket s);

  // The parameter is counted.  It is verified, because it is returned.
  @CreatesMustCallFor("#1")
  static @MustCallAlias Socket resetAndReturn(@MustCallAlias Socket s) {
    // Not counted, because the caller of resetAndReturn counts the reset.
    reset(s);
    return s;
  }
}
