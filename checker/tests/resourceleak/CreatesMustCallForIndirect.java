// A test that methods containing calls to other @CreatesMustCallFor methods work as intended.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a") class CreatesMustCallForIndirect {

  @CreatesMustCallFor
  void reset() {}

  void a() {}

  static @MustCall({}) CreatesMustCallForSimple makeNoMC() {
    return null;
  }

  public static void resetIndirect_no_anno(CreatesMustCallForIndirect r) {
    // :: error: reset.not.owning
    r.reset();
  }

  @CreatesMustCallFor("#1")
  public static void resetIndirect_anno(CreatesMustCallForIndirect r) {
    r.reset();
  }

  public static void reset_local() {
    // :: error: required.method.not.called
    CreatesMustCallForIndirect r = new CreatesMustCallForIndirect();
    r.reset();
  }

  public static void reset_local2() {
    CreatesMustCallForIndirect r = new CreatesMustCallForIndirect();
    r.reset();
    r.a();
  }

  public static void reset_local3() {
    // :: error: required.method.not.called
    CreatesMustCallForIndirect r = new CreatesMustCallForIndirect();
    // Ideally, we'd issue a reset.not.owning error on the next line instead, but not being able to
    // parse
    // the case and requiring it to be in a local var is okay too.
    // :: error: createsmustcallfor.target.unparseable
    ((CreatesMustCallForIndirect) r).reset();
  }

  // :: error: required.method.not.called
  public static void test(@Owning CreatesMustCallForIndirect r) {
    resetIndirect_anno(r);
  }

  public static void test2(CreatesMustCallForIndirect r) {
    // :: error: reset.not.owning
    resetIndirect_anno(r);
  }

  public static void test3(@Owning CreatesMustCallForIndirect r) {
    resetIndirect_anno(r);
    r.a();
  }
}
