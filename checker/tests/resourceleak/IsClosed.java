// A test that the EnsuresCalledMethodsIf annotations in
// the Socket stub files are respected.

import java.io.*;
import java.net.*;
import org.checkerframework.checker.mustcall.qual.Owning;

class IsClosed {
  void test_socket(@Owning Socket sock) {
    if (!sock.isClosed()) {
      try {
        sock.close();
      } catch (IOException io) {

      }
    }
  }

  void test_server_socket(@Owning ServerSocket sock) {
    if (!sock.isClosed()) {
      try {
        sock.close();
      } catch (IOException io) {

      }
    }
  }
}
