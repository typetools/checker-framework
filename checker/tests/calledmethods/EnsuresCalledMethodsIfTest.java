// Test case for https://github.com/typetools/checker-framework/issues/4699

import java.io.IOException;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsIf;

class EnsuresCalledMethodsIfTest {

  @EnsuresCalledMethods(value = "#1", methods = "close")
  // The contract is not satisfied.  Suppose `sock` is null.  Then `sock.close()` throws a
  // NullPointerException before `sock.close()` has a chance to be called.  The exception is caught
  // and control exits the method without `close()` being called.
  // :: error: (contracts.postcondition)
  public static void closeSock(EnsuresCalledMethodsIfTest sock) throws Exception {
    if (!sock.isOpen()) {
      return;
    }
    try {
      sock.close();
    } catch (Exception e) {
    }
  }

  @EnsuresCalledMethods(value = "#1", methods = "close")
  public static void closeSockOK(EnsuresCalledMethodsIfTest sock) throws Exception {
    if (!sock.isOpen()) {
      return;
    }
    try {
      sock.close();
    } catch (IOException e) {
    }
  }

  @EnsuresCalledMethods(value = "#1", methods = "close")
  public static void closeSockOK1(EnsuresCalledMethodsIfTest sock) throws Exception {
    if (!sock.isOpen()) {
      return;
    }
    sock.close();
  }

  @EnsuresCalledMethods(value = "#1", methods = "close")
  public static void closeSockOK2(EnsuresCalledMethodsIfTest sock) throws Exception {
    if (sock.isOpen()) {
      sock.close();
    }
  }

  void close() throws IOException {}

  @SuppressWarnings(
      "calledmethods") // like the JDK's isOpen methods; makes this test case self-contained
  @EnsuresCalledMethodsIf(
      expression = "this",
      result = false,
      methods = {"close"})
  boolean isOpen() {
    return true;
  }
}
