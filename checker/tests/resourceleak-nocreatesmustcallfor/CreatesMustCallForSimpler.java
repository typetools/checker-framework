// A simpler test that @CreatesMustCallFor works as intended wrt the Object Construction Checker.

// This test has been modified to expect that CreatesMustCallFor is feature-flagged to off.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a") class CreatesMustCallForSimpler {

    @CreatesMustCallFor
    void reset() {}

    @CreatesMustCallFor("this")
    void resetThis() {}

    void a() {}

    static @MustCall({}) CreatesMustCallForSimpler makeNoMC() {
        // :: error: (return.type.incompatible)
        return new CreatesMustCallForSimpler();
    }

    static void test1() {
        CreatesMustCallForSimpler cos = makeNoMC();
        @MustCall({}) CreatesMustCallForSimpler a = cos;
        cos.reset();
        @CalledMethods({"reset"}) CreatesMustCallForSimpler b = cos;
        @CalledMethods({}) CreatesMustCallForSimpler c = cos;
    }
}
