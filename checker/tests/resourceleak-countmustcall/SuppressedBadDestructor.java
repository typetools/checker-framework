// Test input for ResourceLeakCountMustCallTest.

import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

@InheritableMustCall("close")
class SuppressedBadDestructor {
  // Counted, but not verified because the destructor does not fulfill its contract.
  final @Owning Socket socket;

  SuppressedBadDestructor(@Owning Socket socket) {
    this.socket = socket;
  }

  @EnsuresCalledMethods(value = "this.socket", methods = "close")
  @SuppressWarnings("contracts.postcondition") // Test that suppressed failures are counted.
  void close() {}
}
