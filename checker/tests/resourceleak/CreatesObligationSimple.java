// A simple test that @CreatesObligation works as intended wrt the Object Construction Checker.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a") class CreatesObligationSimple {

  @CreatesObligation
  void reset() {}

  @CreatesObligation("this")
  void resetThis() {}

  void a() {}

  static @MustCall({}) CreatesObligationSimple makeNoMC() {
    return null;
  }

  static void test1() {
    // :: error: required.method.not.called
    CreatesObligationSimple cos = makeNoMC();
    @MustCall({}) CreatesObligationSimple a = cos;
    cos.reset();
    // :: error: assignment.type.incompatible
    @CalledMethods({"reset"}) CreatesObligationSimple b = cos;
    @CalledMethods({}) CreatesObligationSimple c = cos;
  }

  static void test2() {
    // :: error: required.method.not.called
    CreatesObligationSimple cos = makeNoMC();
    @MustCall({}) CreatesObligationSimple a = cos;
    cos.resetThis();
    // :: error: assignment.type.incompatible
    @CalledMethods({"resetThis"}) CreatesObligationSimple b = cos;
    @CalledMethods({}) CreatesObligationSimple c = cos;
  }

  static void test3() {
    // :: error: required.method.not.called
    CreatesObligationSimple cos = new CreatesObligationSimple();
    cos.a();
    cos.resetThis();
  }

  static void test4() {
    CreatesObligationSimple cos = new CreatesObligationSimple();
    cos.a();
    cos.resetThis();
    cos.a();
  }

  static void test5() {
    CreatesObligationSimple cos = new CreatesObligationSimple();
    cos.resetThis();
    cos.a();
  }

  static void test6(boolean b) {
    CreatesObligationSimple cos = new CreatesObligationSimple();
    if (b) {
      cos.resetThis();
    }
    cos.a();
  }

  static void test7(boolean b) {
    // :: error: required.method.not.called
    CreatesObligationSimple cos = new CreatesObligationSimple();
    cos.a();
    if (b) {
      cos.resetThis();
    }
  }
}
