// Test that, on the exceptional paths out of a method call, the receiver's type reflects the
// method's exceptional postconditions but not its normal postconditions.

import java.io.IOException;
import org.checkerframework.checker.calledmethods.qual.*;

abstract class ExceptionalPathPostconditions {

  abstract static class Resource {
    abstract void close();

    @EnsuresCalledMethods(value = "this", methods = "close")
    abstract void closeOnNormalReturn() throws IOException;

    @EnsuresCalledMethodsOnException(value = "this", methods = "close")
    abstract void closeOnException() throws IOException;
  }

  void normalPostcondition(Resource r) {
    try {
      r.closeOnNormalReturn();
      @CalledMethods({"close", "closeOnNormalReturn"}) Resource r1 = r;
    } catch (IOException e) {
      @CalledMethods("closeOnNormalReturn") Resource r2 = r;
      // :: error: [assignment]
      @CalledMethods("close") Resource r3 = r;
    }
  }

  void exceptionalPostcondition(Resource r) {
    try {
      r.closeOnException();
      @CalledMethods("closeOnException") Resource r1 = r;
      // :: error: [assignment]
      @CalledMethods("close") Resource r2 = r;
    } catch (IOException e) {
      @CalledMethods({"close", "closeOnException"}) Resource r3 = r;
    }
  }
}
