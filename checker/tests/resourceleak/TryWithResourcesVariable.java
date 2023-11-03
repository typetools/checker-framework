// Test for try-with-resources where the resource is a variable rather than a declaration

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

  static void test3(InetSocketAddress isa) {
    Socket socket = new Socket();
    try (socket) {
      // We get a false positive reset.not.owning error here.  MustCallConsistencyAnalyzer has a
      // special-case hack for variables declared in a try-with-resources, but that does not work
      // for variables used in try-with-resources but declared beforehand.
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
