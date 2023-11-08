// Test for try-with-resources where the resource declaration uses an existing variable

import java.net.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class TryWithResourcesVariable {
  static void test1() throws Exception {
    Socket socket = new Socket("127.0.0.1", 5050);
    try (socket) {

    } catch (Exception e) {

    }
  }

  static void test2(@Owning Socket socket) {
    try (socket) {

    } catch (Exception e) {

    }
  }

  static void test3a(InetSocketAddress isa) {
    Socket socket = new Socket();
    try (socket) {
      socket.connect(isa);
    } catch (Exception e) {

    }
  }

  @InheritableMustCall("disposer")
  static class FinalResourceField {
    final @Owning Socket socketField;

    FinalResourceField() {
      try {
        socketField = new Socket("127.0.0.1", 5050);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @EnsuresCalledMethods(value = "this.socketField", methods = "close")
    void disposer() {
      try (socketField) {

      } catch (Exception e) {

      }
    }
  }

  static void closeFinalFieldUnsupported() throws Exception {
    // :: error: (required.method.not.called)
    FinalResourceField finalResourceField = new FinalResourceField();
    try (finalResourceField.socketField) {}
  }

  @InheritableMustCall("disposer")
  static class FinalResourceFieldWrapper {

    final @Owning FinalResourceField frField = new FinalResourceField();

    @EnsuresCalledMethods(value = "this.frField", methods = "disposer")
    void disposer() {
      this.frField.disposer();
    }
  }

  static void closeWrapperUnsupported() throws Exception {
    // :: error: (required.method.not.called)
    FinalResourceFieldWrapper finalResourceFieldWrapper = new FinalResourceFieldWrapper();
    try (finalResourceFieldWrapper.frField.socketField) {}
  }
}
