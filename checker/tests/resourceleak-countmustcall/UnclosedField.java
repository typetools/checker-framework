// Test input for ResourceLeakCountMustCallTest.

import java.net.Socket;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

@InheritableMustCall("close")
class UnclosedField {
  // Counted.  Not verified, because close() does not close it.
  final @Owning Socket s;

  // The parameter is counted and verified.
  UnclosedField(@Owning Socket s) {
    this.s = s;
  }

  void close() {}
}
