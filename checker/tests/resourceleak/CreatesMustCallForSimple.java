// A simple test that @CreatesMustCallFor works as intended wrt the Object Construction Checker.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a") class CreatesMustCallForSimple {

    @CreatesMustCallFor
    void reset() {}

    @CreatesMustCallFor("this")
    void resetThis() {}

    void a() {}

    static @MustCall({}) CreatesMustCallForSimple makeNoMC() {
        return null;
    }

    static void test1() {
        // :: error: required.method.not.called
        CreatesMustCallForSimple cos = makeNoMC();
        @MustCall({}) CreatesMustCallForSimple a = cos;
        cos.reset();
        // :: error: assignment
        @CalledMethods({"reset"}) CreatesMustCallForSimple b = cos;
        @CalledMethods({}) CreatesMustCallForSimple c = cos;
    }

    static void test2() {
        // :: error: required.method.not.called
        CreatesMustCallForSimple cos = makeNoMC();
        @MustCall({}) CreatesMustCallForSimple a = cos;
        cos.resetThis();
        // :: error: assignment
        @CalledMethods({"resetThis"}) CreatesMustCallForSimple b = cos;
        @CalledMethods({}) CreatesMustCallForSimple c = cos;
    }

    static void test3() {
        // :: error: required.method.not.called
        CreatesMustCallForSimple cos = new CreatesMustCallForSimple();
        cos.a();
        cos.resetThis();
    }

    static void test4() {
        CreatesMustCallForSimple cos = new CreatesMustCallForSimple();
        cos.a();
        cos.resetThis();
        cos.a();
    }

    static void test5() {
        CreatesMustCallForSimple cos = new CreatesMustCallForSimple();
        cos.resetThis();
        cos.a();
    }

    static void test6(boolean b) {
        CreatesMustCallForSimple cos = new CreatesMustCallForSimple();
        if (b) {
            cos.resetThis();
        }
        cos.a();
    }

    static void test7(boolean b) {
        // :: error: required.method.not.called
        CreatesMustCallForSimple cos = new CreatesMustCallForSimple();
        cos.a();
        if (b) {
            cos.resetThis();
        }
    }
}
