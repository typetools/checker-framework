// A simpler test that @CreatesObligation works as intended wrt the Object Construction Checker.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a") class CreatesObligationSimpler {

  @CreatesObligation
  void reset() {}

  @CreatesObligation("this")
  void resetThis() {}

  void a() {}

  static @MustCall({}) CreatesObligationSimpler makeNoMC() {
    return null;
  }

  static void test1() {
    // :: error: required.method.not.called
    CreatesObligationSimpler cos = makeNoMC();
    @MustCall({}) CreatesObligationSimpler a = cos;
    cos.reset();
    // :: error: assignment
    @CalledMethods({"reset"}) CreatesObligationSimpler b = cos;
    @CalledMethods({}) CreatesObligationSimpler c = cos;
  }
}
