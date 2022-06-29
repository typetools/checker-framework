import java.io.IOException;
import java.util.function.Function;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

class ACRegularExitPointTest {

  @MustCall("a") class Foo {
    void a() {}

    @This Foo b() {
      return this;
    }

    void c(@CalledMethods("a") Foo this) {}
  }

  @MustCall("a") class SubFoo extends Foo {}

  Foo makeFoo() {
    return new Foo();
  }

  @CalledMethods("a") Foo makeFooCallA() {
    Foo f = new Foo();
    f.a();
    return f;
  }

  @EnsuresCalledMethods(value = "#1", methods = "a")
  void callA(Foo f) {
    f.a();
  }

  void makeFooFinalize() {
    Foo f = new Foo();
    f.a();
  }

  void makeFooFinalizeWrong() {
    Foo m;
    // :: error: required.method.not.called
    m = new Foo();
    // :: error: required.method.not.called
    Foo f = new Foo();
    f.b();
  }

  void testStoringInLocalWrong() {
    // :: error: required.method.not.called
    Foo foo = makeFoo();
  }

  void testStoringInLocalWrong2() {
    Foo f;
    // :: error: required.method.not.called
    f = makeFoo();
  }

  void testStoringInLocal() {
    Foo foo = makeFooCallA();
  }

  void testStoringInLocalWrong3() {
    // :: error: required.method.not.called
    Foo foo = new Foo();
  }

  void emptyFuncWithFormalPram(Foo f) {}

  void innerFunc(Foo f) {
    Runnable r =
        new Runnable() {
          public void run() {
            Foo f;
          }
          ;
        };
    r.run();
  }

  void innerFuncWrong(Foo f) {
    Runnable r =
        new Runnable() {
          public void run() {
            // :: error: required.method.not.called
            Foo g = new Foo();
          }
          ;
        };
    r.run();
  }

  void innerFunc2(Foo f) {
    Runnable r =
        new Runnable() {
          public void run() {
            Foo g = makeFoo();
            g.a();
          }
          ;
        };
    r.run();
  }

  void innerfunc3() {

    Foo f = makeFoo();
    f.a();
    Function<Foo, @CalledMethods("a") Foo> innerfunc =
        st -> {
          // :: error: required.method.not.called
          Foo fn1 = new Foo();
          Foo fn2 = makeFoo();
          fn2.a();
          return fn2;
        };

    innerfunc.apply(f);
  }

  void ifElse(boolean b) {
    if (b) {
      Foo f1 = new Foo();
      f1.a();
    } else {
      // :: error: required.method.not.called
      Foo f2 = new Foo();
    }
  }

  Foo ifElseWithReturnExit(boolean b, boolean c) {
    // :: error: required.method.not.called
    Foo f1 = makeFoo();
    // :: error: required.method.not.called
    Foo f3 = new Foo();
    // :: error: required.method.not.called
    Foo f4 = new Foo();

    if (b) {
      // :: error: required.method.not.called
      Foo f2 = new Foo();
      if (c) {
        f4.a();
      } else {
        f4.b();
      }
      return f1;
    } else {
      // :: error: required.method.not.called
      Foo f2 = new Foo();
      f2 = new Foo();
      f2.a();
    }
    return f3;
  }

  void ifElseWithDeclaration(boolean b) {
    Foo f1;
    Foo f2;
    if (b) {
      f1 = new Foo();
      f1.a();
    } else {
      // :: error: required.method.not.called
      f2 = new Foo();
    }
  }

  void ifElseWithInitialization(boolean b) {
    // :: error: required.method.not.called
    Foo f2 = new Foo();
    Foo f11 = null;
    if (b) {
      f11 = makeFoo();
      f11.a();
    } else {
      // :: error: required.method.not.called
      f2 = new Foo();
    }
  }

  void ifWithInitialization(boolean b) {
    // :: error: required.method.not.called
    Foo f1 = new Foo();
    // :: error: required.method.not.called
    Foo f2 = new Foo();
    if (b) {
      f1.a();
    }
  }

  void variableGoesOutOfScope(boolean b) {
    if (b) {
      Foo f1 = new Foo();
      f1.a();
    }
  }

  void ifWithNullInitialization(boolean b) {
    Foo f1 = null;
    Foo f2 = null;
    if (b) {
      f1 = new Foo();
      f1.a();
    } else {
      // :: error: required.method.not.called
      f2 = new Foo();
    }
  }

  void variableInitializedWithNull() {
    Foo f = null;
  }

  void testLoop() {
    Foo f = null;
    while (true) {
      // :: error: required.method.not.called
      f = new Foo();
    }
  }

  void overWrittingVarInLoop() {
    // :: error: required.method.not.called
    Foo f = new Foo();
    while (true) {
      // :: error: required.method.not.called
      f = new Foo();
    }
  }

  void loopWithNestedBranches(boolean b) {
    Foo frodo = null;
    while (true) {
      if (b) {
        // :: error: required.method.not.called
        frodo = new Foo();
      } else {
        // this is a known false positive, due to lack of path sensitivity in the
        // Called Methods Checker
        // :: error: required.method.not.called
        frodo = new Foo();
        frodo.a();
      }
    }
  }

  void replaceVarWithNull(boolean b, boolean c) {
    // :: error: required.method.not.called
    Foo f = new Foo();
    if (b) {
      f = null;
    } else if (c) {
      f = null;
    } else {

    }
  }

  void ownershipTransfer() {
    Foo f1 = new Foo();
    Foo f2 = f1;
    Foo f3 = f2.b();
    f3.a();
  }

  void ownershipTransfer2() {
    Foo f1 = null;
    Foo f2 = f1;
  }

  void testECM() {
    Foo f = new Foo();
    callA(f);
  }

  void testFinallyBlock(boolean b) {
    Foo f = null;
    try {
      f = new Foo();
      if (true) {
        throw new IOException();
      }
    } catch (IOException e) {

    } finally {
      f.a();
    }
  }

  void testSubFoo() {
    // :: error: required.method.not.called
    Foo f = new SubFoo();
  }

  void testSubFoo2() {
    // :: error: required.method.not.called
    SubFoo f = new SubFoo();
  }
}
