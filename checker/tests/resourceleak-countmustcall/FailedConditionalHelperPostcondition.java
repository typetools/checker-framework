// Test input for ResourceLeakCountMustCallTest.

import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsIf;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

@InheritableMustCall("close")
class FailedConditionalHelperPostcondition {
  // Counted, but not verified because tryClose(), on which close() relies, does not fulfill its
  // conditional contract.
  final @Owning Socket socket;

  FailedConditionalHelperPostcondition(@Owning Socket socket) {
    this.socket = socket;
  }

  @EnsuresCalledMethods(value = "this.socket", methods = "close")
  void close() throws IOException {
    if (!tryClose()) {
      socket.close();
    }
  }

  @EnsuresCalledMethodsIf(expression = "this.socket", methods = "close", result = true)
  boolean tryClose() {
    return true;
  }
}
