// An unparsable `@EnsuresCalledMethodsOnException` expression is reported at the call as well as
// at the declaration.  The callee may be declared in a stub file or in another compilation unit,
// in which case the declaration is never checked and the call is the only place to report it.

import java.io.IOException;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsOnException;

public class EnsuresCalledMethodsOnExceptionUnparsable {

  static class Resource {
    void a() {}
  }

  @EnsuresCalledMethodsOnException(value = "nosuchthing", methods = "a")
  // :: error: (flowexpr.parse.error)
  void unparsable(Resource r) throws IOException {
    throw new IOException();
  }

  void call(Resource r) throws IOException {
    // :: error: (flowexpr.parse.error.postcondition)
    unparsable(r);
  }
}
