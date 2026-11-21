// Test input for ResourceLeakCountMustCallTest.

import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

@InheritableMustCall("close")
class IncompatibleCreatesMustCallFor {
  // Counted.  Not verified, because replace() has @CreatesMustCallFor for the wrong target.
  private @Owning Socket s;

  // The parameter is counted and verified.
  IncompatibleCreatesMustCallFor(@Owning Socket s) {
    this.s = s;
  }

  // An unconnected socket has no must-call obligation, so the new socket is not counted.
  @CreatesMustCallFor("#1")
  // :: error: (incompatible.creates.mustcall.for)
  void replace(IncompatibleCreatesMustCallFor other) throws IOException {
    s.close();
    this.s = new Socket();
  }

  @EnsuresCalledMethods(value = "this.s", methods = "close")
  void close() throws IOException {
    s.close();
  }
}
