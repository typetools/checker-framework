// A call tests its receiver for nullness after evaluating its arguments, but before the method
// runs.  So a handler for the NullPointerException that a null receiver causes must not assume the
// method's exceptional postconditions.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;

abstract class ReceiverNullBeforeDispatch {

  @EnsuresCalledMethodsOnException(value = "#1", methods = "close")
  abstract void closeOnException(Closeable c);

  static void nullableReceiver(ReceiverNullBeforeDispatch r) throws IOException {
    // If `r` is null, closeOnException never runs, so `c` is not closed in the handler.
    // ::error: (required.method.not.called)
    Closeable c = new FileInputStream("file.txt");
    try {
      r.closeOnException(c);
    } catch (NullPointerException e) {
      return;
    }
    c.close();
  }

  void thisReceiver() throws IOException {
    // `this` is never null, so the handler is reached only if closeOnException throws.
    Closeable c = new FileInputStream("file.txt");
    try {
      this.closeOnException(c);
    } catch (NullPointerException e) {
      return;
    }
    c.close();
  }
}
