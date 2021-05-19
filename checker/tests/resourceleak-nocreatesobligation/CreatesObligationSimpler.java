// A simpler test that @CreatesObligation works as intended wrt the Object Construction Checker.

// This test has been modified to expect that CreatesObligation is feature-flagged to off.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a") class CreatesObligationSimpler {

  @CreatesObligation
  void reset() {}

  @CreatesObligation("this")
  void resetThis() {}

  void a() {}

  static @MustCall({}) CreatesObligationSimpler makeNoMC() {
    // :: error: return
    return new CreatesObligationSimpler();
  }

  static void test1() {
    CreatesObligationSimpler cos = makeNoMC();
    @MustCall({}) CreatesObligationSimpler a = cos;
    cos.reset();
    @CalledMethods({"reset"}) CreatesObligationSimpler b = cos;
    @CalledMethods({}) CreatesObligationSimpler c = cos;
  }
}
