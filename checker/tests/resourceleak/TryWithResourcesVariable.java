// Test for try-with-resources where the resource is a variable rather than a declaration

import java.net.Socket;
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

  @InheritableMustCall("disposer")
  static class FinalResourceField {
    final Socket socketField;

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
    try (new FinalResourceField().socketField) {}
  }

  static class FinalResourceFieldWrapper {
    final FinalResourceField frField = new FinalResourceField();
  }

  static void closeWrapperUnsupported() throws Exception {
    try (new FinalResourceFieldWrapper().frField.socketField) {}
  }
}
