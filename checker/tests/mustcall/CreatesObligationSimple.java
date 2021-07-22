// A simple test that @CreatesObligation works as intended wrt the Must Call Checker.

import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a") class CreatesObligationSimple {

    @CreatesObligation
    void reset() {}

    @CreatesObligation("this")
    void resetThis() {}

    static @MustCall({}) CreatesObligationSimple makeNoMC() {
        return null;
    }

    static void test1() {
        CreatesObligationSimple cos = makeNoMC();
        @MustCall({}) CreatesObligationSimple a = cos;
        cos.reset();
        // :: error: assignment
        @MustCall({}) CreatesObligationSimple b = cos;
        @MustCall("a") CreatesObligationSimple c = cos;
    }

    static void test2() {
        CreatesObligationSimple cos = makeNoMC();
        @MustCall({}) CreatesObligationSimple a = cos;
        cos.resetThis();
        // :: error: assignment
        @MustCall({}) CreatesObligationSimple b = cos;
        @MustCall("a") CreatesObligationSimple c = cos;
    }

    static void test3() {
        Object cos = makeNoMC();
        @MustCall({}) Object a = cos;
        // :: error: createsobligation.target.unparseable
        ((CreatesObligationSimple) cos).reset();
        // It would be better to issue an assignment incompatible error here, but the
        // error above is okay too.
        @MustCall({}) Object b = cos;
        @MustCall("a") Object c = cos;
    }

    // Rewrite of test3 that follows the instructions in the error message.
    static void test4() {
        Object cos = makeNoMC();
        @MustCall({}) Object a = cos;
        CreatesObligationSimple r = ((CreatesObligationSimple) cos);
        r.reset();
        // :: error: assignment
        @MustCall({}) Object b = r;
        @MustCall("a") Object c = r;
    }
}
