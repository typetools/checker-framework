// Test case for https://github.com/kelloggm/object-construction-checker/issues/368

import org.checkerframework.checker.mustcall.qual.*;

class CreatesMustCallForInnerClass {
    @InheritableMustCall("foo")
    static class Foo {

        void foo() {}

        @CreatesMustCallFor("this")
        void resetFoo() {}

        /** non-static inner class */
        class Bar {
            @CreatesMustCallFor
            // :: error: creates.mustcall.for.invalid.target
            void bar() {
                // :: error: reset.not.owning
                resetFoo();
            }
        }

        void callBar() {
            Bar b = new Bar();
            // If this call to bar() were permitted with no errors, this would be unsound.
            // b is in fact an owning pointer, but we don't track it as such because
            // Bar objects cannot have must-call obligations created for them.
            // :: error: reset.not.owning
            b.bar();
        }
    }
}
