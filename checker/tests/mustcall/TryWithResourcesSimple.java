// A test that try-with-resources variables are always @MustCall({"close"}).

import java.io.*;
import java.net.*;
import org.checkerframework.checker.mustcall.qual.MustCall;

public class TryWithResourcesSimple {
  static void test(String address, int port) {
    try (Socket socket = new Socket(address, port)) {
      @MustCall({"close"}) Object s = socket;
    } catch (Exception e) {

    }
  }

  @SuppressWarnings("mustcall:annotations.on.use")
  public static @MustCall({"close", "myMethod"}) Socket getFancySocket() {
    return null;
  }

  void test_fancy_sock(String address, int port) {
    // This is illegal, because getFancySock()'s return type has another MC method beyond
    // "close", which is the only MC method for Socket itself.
    try (Socket socket = getFancySocket()) {
      // :: error: (assignment)
      @MustCall({"close"}) Object s = socket;
    } catch (Exception e) {

    }
  }

  static void test_poly(String address, int port) {
    try (Socket socket = new Socket(address, port)) {
      // getChannel is @MustCallAlias (= poly) with the socket, so it should also be
      // @MC({"close"})
      @MustCall({"close"}) Object s = socket.getChannel();
    } catch (Exception e) {

    }
  }

  static void test_two_mca_variables(String address, int port) {
    try (Socket socket = new Socket(address, port);
        InputStream in = socket.getInputStream()) {
      @MustCall({"close"}) Object s = in;
    } catch (Exception e) {

    }
  }
}
