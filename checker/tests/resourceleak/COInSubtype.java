// A test for a bad interaction between CO and subtyping
// that could happen if CO was unsound.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class COInSubtype {
    static class Foo {

        void foo() {}

        // This is not supported, even though a sub-class may have must-call obligations.
        // This pattern is not used in realistic code, and supporting it hurts checker performance.
        @CreatesMustCallFor("this")
        // :: error: creates.mustcall.for.invalid.target
        void resetFoo() {}
    }

    @InheritableMustCall("a")
    static class Bar extends Foo {
        void a() {}
    }

    static void test() {
        // :: error: required.method.not.called
        @MustCall("a") Foo f = new Bar();
        f.resetFoo();
    }
}
