import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

class ACMethodInvocationTest {

    @MustCall("a") class Foo {
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

    @CalledMethods({"a"}) Foo makeFooFinalize() {
        Foo f = new Foo();
        f.a();
        return f;
    }

    @Owning
    @CalledMethods({"b"}) Foo makeFooFinalize2() {
        Foo f = new Foo();
        f.b();
        return f;
    }

    void CallMethodsInSequence() {
        makeFoo().a();
    }

    void CallMethodsInSequence2() {
        makeFoo().b().a();
    }

    void testFluentAPIWrong() {
        // :: error: required.method.not.called
        makeFoo().b();
    }

    void testFluentAPIWrong2() {
        // :: error: required.method.not.called
        makeFoo();
    }

    void invokeMethodWithCallA() {
        makeFooFinalize();
    }

    void invokeMethodWithCallBWrong() {
        // :: error: required.method.not.called
        makeFooFinalize2();
    }

    void invokeMethodAndCallCWrong() {
        // :: error: required.method.not.called
        makeFoo().c();
    }

    Foo returnMakeFoo() {
        return makeFoo();
    }

    Foo testField1;
    Foo testField2;
    Foo testField3;

    void testStoringInField() {
        // :: error: required.method.not.called
        testField1 = makeFoo();
        // :: error: required.method.not.called
        testField2 = new Foo();

        testField3 = makeFooFinalize();
    }

    void tryCatchFinally() {
        Foo f = null;
        try {
            f = new Foo();
            try {
                throw new RuntimeException();
            } catch (Exception e) {

            }
        } finally {
            f.a();
        }
    }
}
