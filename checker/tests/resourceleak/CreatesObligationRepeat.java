// A simple test that @CreatesObligation is repeatable and works as intended.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a") class CreatesObligationRepeat {

  @CreatesObligation("this")
  @CreatesObligation("#1")
  void reset(CreatesObligationRepeat r) {}

  void a() {}

  static @MustCall({}) CreatesObligationRepeat makeNoMC() {
    return null;
  }

  static void test1() {
    // :: error: required.method.not.called
    CreatesObligationRepeat cos1 = makeNoMC();
    // :: error: required.method.not.called
    CreatesObligationRepeat cos2 = makeNoMC();
    @MustCall({}) CreatesObligationRepeat a = cos2;
    @MustCall({}) CreatesObligationRepeat a2 = cos2;
    cos2.a();
    cos1.reset(cos2);
    // :: error: assignment
    @CalledMethods({"reset"}) CreatesObligationRepeat b = cos1;
    @CalledMethods({}) CreatesObligationRepeat c = cos1;
    @CalledMethods({}) CreatesObligationRepeat d = cos2;
    // :: error: assignment
    @CalledMethods({"a"}) CreatesObligationRepeat e = cos2;
  }

  static void test3() {
    // :: error: required.method.not.called
    CreatesObligationRepeat cos = new CreatesObligationRepeat();
    // :: error: required.method.not.called
    CreatesObligationRepeat cos2 = new CreatesObligationRepeat();
    cos.a();
    cos.reset(cos2);
  }

  static void test4() {
    CreatesObligationRepeat cos = new CreatesObligationRepeat();
    // :: error: required.method.not.called
    CreatesObligationRepeat cos2 = new CreatesObligationRepeat();
    cos.a();
    cos.reset(cos2);
    cos.a();
  }

  static void test5() {
    // :: error: required.method.not.called
    CreatesObligationRepeat cos = new CreatesObligationRepeat();
    CreatesObligationRepeat cos2 = new CreatesObligationRepeat();
    cos.a();
    cos.reset(cos2);
    cos2.a();
  }

  static void test6() {
    CreatesObligationRepeat cos = new CreatesObligationRepeat();
    CreatesObligationRepeat cos2 = new CreatesObligationRepeat();
    cos.a();
    cos.reset(cos2);
    cos2.a();
    cos.a();
  }
}
