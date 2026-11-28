// Test input for ResourceLeakCountMustCallTest.

import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

@InheritableMustCall("close")
class OverwrittenField {
  // Counted.  Not verified, because replace() overwrites it.
  @Owning Socket s;

  // The parameter is counted and verified.
  OverwrittenField(@Owning Socket s) {
    this.s = s;
  }

  // The parameter is counted and verified.
  @CreatesMustCallFor("this")
  void replace(@Owning Socket t) {
    this.s = t;
  }

  @EnsuresCalledMethods(value = "this.s", methods = "close")
  void close() throws IOException {
    s.close();
  }
}
