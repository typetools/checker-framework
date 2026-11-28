// Test input for ResourceLeakCountMustCallTest.

import java.io.IOException;
import java.io.OutputStream;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

@InheritableMustCall("close")
class UnrelatedPostcondition {
  // Counted and verified: close() calls close(), although it does not call flush().
  final @Owning OutputStream stream;

  UnrelatedPostcondition(@Owning OutputStream stream) {
    this.stream = stream;
  }

  @EnsuresCalledMethods(
      value = "this.stream",
      methods = {"close", "flush"})
  void close() throws IOException {
    stream.close();
  }
}
