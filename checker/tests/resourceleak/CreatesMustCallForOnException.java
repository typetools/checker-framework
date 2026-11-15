// Test that an @CreatesMustCallFor method resets the called methods of its target on the
// exceptional paths out of a call, as well as on the normal path.

import java.io.IOException;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("a")
class CreatesMustCallForOnException {

  @CreatesMustCallFor("this")
  void reset() throws IOException {}

  void a() {}

  static void test(@Owning CreatesMustCallForOnException r) {
    r.a();
    try {
      r.reset();
      // :: error: [assignment]
      @CalledMethods("a") CreatesMustCallForOnException r1 = r;
    } catch (IOException e) {
      // :: error: [assignment]
      @CalledMethods("a") CreatesMustCallForOnException r2 = r;
    }
    r.a();
  }
}
