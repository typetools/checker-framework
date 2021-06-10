// A simple test that @CreatesMustCallFor works as intended wrt the Must Call Checker.

import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a") class CreatesMustCallForSimple {

  @CreatesMustCallFor
  void reset() {}

  @CreatesMustCallFor("this")
  void resetThis() {}

  static @MustCall({}) CreatesMustCallForSimple makeNoMC() {
    return null;
  }

  static void test1() {
    CreatesMustCallForSimple cos = makeNoMC();
    @MustCall({}) CreatesMustCallForSimple a = cos;
    cos.reset();
    // :: error: assignment
    @MustCall({}) CreatesMustCallForSimple b = cos;
    @MustCall("a") CreatesMustCallForSimple c = cos;
  }

  static void test2() {
    CreatesMustCallForSimple cos = makeNoMC();
    @MustCall({}) CreatesMustCallForSimple a = cos;
    cos.resetThis();
    // :: error: assignment
    @MustCall({}) CreatesMustCallForSimple b = cos;
    @MustCall("a") CreatesMustCallForSimple c = cos;
  }

  static void test3() {
    Object cos = makeNoMC();
    @MustCall({}) Object a = cos;
    // :: error: createsmustcallfor.target.unparseable
    ((CreatesMustCallForSimple) cos).reset();
    // It would be better to issue an assignment incompatible error here, but the
    // error above is okay too.
    @MustCall({}) Object b = cos;
    @MustCall("a") Object c = cos;
  }

  // Rewrite of test3 that follows the instructions in the error message.
  static void test4() {
    Object cos = makeNoMC();
    @MustCall({}) Object a = cos;
    CreatesMustCallForSimple r = ((CreatesMustCallForSimple) cos);
    r.reset();
    // :: error: assignment
    @MustCall({}) Object b = r;
    @MustCall("a") Object c = r;
  }
}
