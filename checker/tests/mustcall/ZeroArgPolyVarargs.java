// Test case for a crash when a method with a @PolyMustCall varargs parameter is called with no
// varargs arguments.

import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;
import org.checkerframework.checker.mustcall.qual.PolyMustCall;

class ZeroArgPolyVarargs {

  static @PolyMustCall Object poly(@PolyMustCall Object... args) {
    return null;
  }

  void zeroArgs() {
    poly();
    @MustCallUnknown Object unknown = poly();
    // :: error: (assignment)
    @MustCall({}) Object nothing = poly();
  }

  void oneArg(@MustCall({}) Object arg) {
    @MustCall({}) Object nothing = poly(arg);
  }
}
