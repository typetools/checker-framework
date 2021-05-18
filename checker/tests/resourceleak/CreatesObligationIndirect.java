// A test that methods containing calls to other @CreatesObligation methods work as intended.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a") class CreatesObligationIndirect {

  @CreatesObligation
  void reset() {}

  void a() {}

  static @MustCall({}) CreatesObligationSimple makeNoMC() {
    return null;
  }

  public static void resetIndirect_no_anno(CreatesObligationIndirect r) {
    // :: error: reset.not.owning
    r.reset();
  }

  @CreatesObligation("#1")
  public static void resetIndirect_anno(CreatesObligationIndirect r) {
    r.reset();
  }

  public static void reset_local() {
    // :: error: required.method.not.called
    CreatesObligationIndirect r = new CreatesObligationIndirect();
    r.reset();
  }

  public static void reset_local2() {
    CreatesObligationIndirect r = new CreatesObligationIndirect();
    r.reset();
    r.a();
  }

  public static void reset_local3() {
    // :: error: required.method.not.called
    CreatesObligationIndirect r = new CreatesObligationIndirect();
    // Ideally, we'd issue a reset.not.owning error on the next line instead, but not being able to
    // parse
    // the case and requiring it to be in a local var is okay too.
    // :: error: mustcall.not.parseable
    ((CreatesObligationIndirect) r).reset();
  }

  // :: error: required.method.not.called
  public static void test(@Owning CreatesObligationIndirect r) {
    resetIndirect_anno(r);
  }

  public static void test2(CreatesObligationIndirect r) {
    // :: error: reset.not.owning
    resetIndirect_anno(r);
  }

  public static void test3(@Owning CreatesObligationIndirect r) {
    resetIndirect_anno(r);
    r.a();
  }
}
