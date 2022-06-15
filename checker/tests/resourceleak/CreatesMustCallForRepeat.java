// A simple test that @CreatesMustCallFor is repeatable and works as intended.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a") class CreatesMustCallForRepeat {

  @CreatesMustCallFor("this")
  @CreatesMustCallFor("#1")
  void reset(CreatesMustCallForRepeat r) {}

  void a() {}

  static @MustCall({}) CreatesMustCallForRepeat makeNoMC() {
    return null;
  }

  static void test1() {
    // :: error: required.method.not.called
    CreatesMustCallForRepeat cos1 = makeNoMC();
    // :: error: required.method.not.called
    CreatesMustCallForRepeat cos2 = makeNoMC();
    @MustCall({}) CreatesMustCallForRepeat a = cos2;
    @MustCall({}) CreatesMustCallForRepeat a2 = cos2;
    cos2.a();
    cos1.reset(cos2);
    // :: error: assignment
    @CalledMethods({"reset"}) CreatesMustCallForRepeat b = cos1;
    @CalledMethods({}) CreatesMustCallForRepeat c = cos1;
    @CalledMethods({}) CreatesMustCallForRepeat d = cos2;
    // :: error: assignment
    @CalledMethods({"a"}) CreatesMustCallForRepeat e = cos2;
  }

  static void test3() {
    // :: error: required.method.not.called
    CreatesMustCallForRepeat cos = new CreatesMustCallForRepeat();
    // :: error: required.method.not.called
    CreatesMustCallForRepeat cos2 = new CreatesMustCallForRepeat();
    cos.a();
    cos.reset(cos2);
  }

  static void test4() {
    CreatesMustCallForRepeat cos = new CreatesMustCallForRepeat();
    // :: error: required.method.not.called
    CreatesMustCallForRepeat cos2 = new CreatesMustCallForRepeat();
    cos.a();
    cos.reset(cos2);
    cos.a();
  }

  static void test5() {
    // :: error: required.method.not.called
    CreatesMustCallForRepeat cos = new CreatesMustCallForRepeat();
    CreatesMustCallForRepeat cos2 = new CreatesMustCallForRepeat();
    cos.a();
    cos.reset(cos2);
    cos2.a();
  }

  static void test6() {
    CreatesMustCallForRepeat cos = new CreatesMustCallForRepeat();
    CreatesMustCallForRepeat cos2 = new CreatesMustCallForRepeat();
    cos.a();
    cos.reset(cos2);
    cos2.a();
    cos.a();
  }
}
