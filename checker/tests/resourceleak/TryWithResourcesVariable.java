// Test for try-with-resources where the resource declaration uses an existing variable

import java.io.*;
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
      socket.connect(isa);
    } catch (Exception e) {

    }
  }

  // :: error: (required.method.not.called)
  static void test4(@Owning InputStream i1, @Owning InputStream i2) {
    try {
      try (i2) {}
      // This will not run if i2.close() throws an IOException
      i1.close();
    } catch (Exception e) {

    }
  }

  static void test4Fixed(@Owning InputStream i1, @Owning InputStream i2) throws IOException {
    try {
      try (i2) {}
    } catch (Exception e) {
    }
    i1.close();
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

  @InheritableMustCall("disposer")
  static class TwoFinalResourceFields {
    final @Owning Socket socketField1;
    final @Owning Socket socketField2;

    TwoFinalResourceFields(@Owning Socket socket1, @Owning Socket socket2) {
      socketField1 = socket1;
      socketField2 = socket2;
    }

    @EnsuresCalledMethods(value = "this.socketField1", methods = "close")
    @EnsuresCalledMethods(value = "this.socketField2", methods = "close")
    void disposer() {
      try (socketField1;
          socketField2) {

      } catch (Exception e) {

      }
    }
  }
}
