// Test input for ResourceLeakCountMustCallTest.

import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

@InheritableMustCall("close")
class FailedHelperPostcondition {
  // Counted, but not verified because closeAll(), on which close() relies, does not fulfill its
  // contract.
  final @Owning Socket socket;

  FailedHelperPostcondition(@Owning Socket socket) {
    this.socket = socket;
  }

  @EnsuresCalledMethods(value = "this.socket", methods = "close")
  void close() {
    closeAll();
  }

  @EnsuresCalledMethods(value = "this.socket", methods = "close")
  void closeAll() {}
}
