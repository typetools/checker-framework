// tests that require the Returns Receiver Checker to be enabled.
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

class ReturnsReceiverExamples {

  @InheritableMustCall("a")
  class Foo {
    void a() {}

    @This Foo b() {
      return this;
    }

    void c() {}
  }

  @Owning
  Foo makeFoo() {
    return new Foo();
  }

  @Owning
  @CalledMethods({"b"}) Foo makeFooFinalize2() {
    Foo f = new Foo();
    f.b();
    return f;
  }

  void CallMethodsInSequence2() {
    makeFoo().b().a();
  }

  void testFluentAPIWrong() {
    // :: error: (required.method.not.called)
    makeFoo().b();
  }

  void testFluentAPIWrong2() {
    // :: error: (required.method.not.called)
    makeFoo();
  }

  @CalledMethods({"a"}) Foo makeFooFinalize() {
    Foo f = new Foo();
    f.a();
    return f;
  }

  void invokeMethodWithCallA() {
    makeFooFinalize();
  }

  void invokeMethodWithCallBWrong() {
    // :: error: (required.method.not.called)
    makeFooFinalize2();
  }

  void invokeMethodAndCallCWrong() {
    // :: error: (required.method.not.called)
    makeFoo().c();
  }

  void makeFooFinalizeWrong() {
    Foo m;
    // :: error: (required.method.not.called)
    m = new Foo();
    // :: error: (required.method.not.called)
    Foo f = new Foo();
    f.b();
  }

  Foo ifElseWithReturnExit(boolean b, boolean c) {
    // :: error: (required.method.not.called)
    Foo f1 = makeFoo();
    // :: error: (required.method.not.called)
    Foo f3 = new Foo();
    // :: error: (required.method.not.called)
    Foo f4 = new Foo();

    if (b) {
      // :: error: (required.method.not.called)
      Foo f2 = new Foo();
      if (c) {
        f4.a();
      } else {
        f4.b();
      }
      return f1;
    } else {
      // :: error: (required.method.not.called)
      Foo f2 = new Foo();
      f2 = new Foo();
      f2.a();
    }
    return f3;
  }

  void ownershipTransfer() {
    Foo f1 = new Foo();
    Foo f2 = f1;
    Foo f3 = f2.b();
    f3.a();
  }
}
