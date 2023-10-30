// Test for try-with-resources where the resource is a variable rather than a declaration

import java.net.Socket;
import org.checkerframework.checker.mustcall.qual.Owning;

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

  static class FinalResourceField {
    private final Socket socketField;

    FinalResourceField() throws Exception {
      socketField = new Socket("127.0.0.1", 5050);
    }

    void disposer() {
      try (socketField) {

      } catch (Exception e) {

      }
    }
  }
}
