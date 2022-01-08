import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

class TernaryExpressions {

    @MustCall("a") class Foo {
        void a() {}

        @This Foo b() {
            return this;
        }

        void c(@CalledMethods("a") Foo this) {}
    }

    Foo makeFoo() {
        return new Foo();
    }

    static void takeOwnership(@Owning Foo foo) {
        foo.a();
    }

    /** cases where ternary expressions are assigned to a variable */
    void testTernaryAssigned(boolean b) {
        Foo ternary1 = b ? new Foo() : makeFoo();
        ternary1.a();

        // :: error: required.method.not.called
        Foo ternary2 = b ? new Foo() : makeFoo();

        // :: error: required.method.not.called
        Foo x = new Foo();
        Foo ternary3 = b ? new Foo() : x;
        ternary3.a();

        Foo y = new Foo();
        Foo ternary4 = b ? y : y;
        ternary4.a();

        takeOwnership(b ? new Foo() : makeFoo());

        // :: error: required.method.not.called
        Foo x2 = new Foo();
        takeOwnership(b ? x2 : null);

        int i = 10;
        Foo ternaryInLoop = null;
        while (i > 0) {
            // :: error: required.method.not.called
            ternaryInLoop = b ? null : new Foo();
            i--;
        }
        ternaryInLoop.a();

        (b ? new Foo() : makeFoo()).a();
    }

    /**
     * tests where ternary and cast expressions (possibly nested) may or may not be assigned to a
     * variable
     */
    void testTernaryCastUnassigned(boolean b) {
        // :: error: required.method.not.called
        if ((b ? new Foo() : null) != null) {
            b = !b;
        }

        // :: error: required.method.not.called
        if ((b ? makeFoo() : null) != null) {
            b = !b;
        }

        Foo x = new Foo();
        if ((b ? x : null) != null) {
            b = !b;
        }
        x.a();

        // :: error: required.method.not.called
        if (((Foo) new Foo()) != null) {
            b = !b;
        }

        // double cast; no error
        Foo doubleCast = (Foo) ((Foo) makeFoo());
        doubleCast.a();

        // nesting casts and ternary expressions; no error
        Foo deepNesting = (b ? (!b ? makeFoo() : (Foo) makeFoo()) : ((Foo) new Foo()));
        deepNesting.a();
    }

    @Owning
    Foo testTernaryReturnOk(boolean b) {
        return b ? new Foo() : makeFoo();
    }

    @Owning
    Foo testTernaryReturnBad(boolean b) {
        // :: error: required.method.not.called
        Foo x = new Foo();
        return b ? x : makeFoo();
    }

    @MustCall("toString") static class Sub1 extends Object {}

    @MustCall("clone") static class Sub2 extends Object {}

    static void testTernarySubtyping(boolean b) {
        // :: error: required.method.not.called
        Object toStringAndClone = b ? new Sub1() : new Sub2();
        // at this point, for soundness, we should be responsible for calling both toString and
        // clone on
        // obj...
        toStringAndClone.toString();
    }
}
