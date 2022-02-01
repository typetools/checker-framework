// @below-java17-jdk-skip-test
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

class SwitchExpressions {

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

    /** cases where switch expressions are assigned to a variable */
    void testSwitchAssigned(int i) {
        Foo switch1 =
                switch (i) {
                    case 3 -> new Foo();
                    default -> makeFoo();
                };
        switch1.a();

        // :: error: required.method.not.called
        Foo switch2 =
                switch (i) {
                    case 3 -> new Foo();
                    default -> makeFoo();
                };

        // :: error: required.method.not.called
        Foo x = new Foo();
        Foo switch3 =
                switch (i) {
                    case 3 -> new Foo();
                    default -> x;
                };
        switch3.a();

        Foo y = new Foo();
        Foo switch4 =
                switch (i) {
                    case 3 -> y;
                    default -> y;
                };
        switch4.a();

        takeOwnership(
                switch (i) {
                    case 3 -> new Foo();
                    default -> makeFoo();
                });

        // :: error: required.method.not.called
        Foo x2 = new Foo();
        takeOwnership(
                switch (i) {
                    case 3 -> x2;
                    default -> null;
                });

        int j = 10;
        Foo switchInLoop = null;
        while (j > 0) {
            // :: error: required.method.not.called
            switchInLoop =
                    switch (i) {
                        case 3 -> null;
                        default -> new Foo();
                    };
            j--;
        }
        switchInLoop.a();

        (switch (i) {
                    case 3 -> new Foo();
                    default -> makeFoo();
                })
                .a();
    }

    /**
     * tests where switch and cast expressions (possibly nested) may or may not be assigned to a
     * variable
     */
    void testSwitchCastUnassigned(int i) {
        // :: error: required.method.not.called
        if ((switch (i) {
                    case 3 -> new Foo();
                    default -> null;
                })
                != null) {
            i = -i;
        }

        // :: error: required.method.not.called
        if (switch (i) {
                    case 3 -> makeFoo();
                    default -> null;
                }
                != null) {
            i = -i;
        }

        Foo x = new Foo();
        if (switch (i) {
                    case 3 -> x;
                    default -> null;
                }
                != null) {
            i = -i;
        }
        x.a();

        // :: error: required.method.not.called
        if (((Foo) new Foo()) != null) {
            i = -i;
        }

        // double cast; no error
        Foo doubleCast = (Foo) ((Foo) makeFoo());
        doubleCast.a();

        // nesting casts and switch expressions; no error
        Foo deepNesting =
                (switch (i) {
                    case 3 -> (switch (-i) {
                        case -3 -> makeFoo();
                        default -> (Foo) makeFoo();
                    });
                    default -> ((Foo) new Foo());
                });
        deepNesting.a();
    }

    @Owning
    Foo testSwitchReturnOk(int i) {
        return switch (i) {
            case 3 -> new Foo();
            default -> makeFoo();
        };
    }

    @Owning
    Foo testSwitchReturnBad(int i) {
        // :: error: required.method.not.called
        Foo x = new Foo();
        return switch (i) {
            case 3 -> x;
            default -> makeFoo();
        };
    }

    @MustCall("toString") static class Sub1 extends Object {}

    @MustCall("clone") static class Sub2 extends Object {}

    static void testSwitchSubtyping(int i) {
        // :: error: required.method.not.called
        Object toStringAndClone =
                switch (i) {
                    case 3 -> new Sub1();
                    default -> new Sub2();
                };
        // at this point, for soundness, we should be responsible for calling both toString and
        // clone on
        // obj...
        toStringAndClone.toString();
    }
}
