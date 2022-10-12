// A test that ensures that the Resource Leak Checker continues to track an
// Object if it's returned from a method with a non-empty @MustCall annotation.

import org.checkerframework.checker.mustcall.qual.MustCall;

public class ConstructorAddsMustCall {
    static class Foo {
        void a() {}

        Foo() {}

        @MustCall("a") Foo(String s) {}
    }

    static void useFoo() {
        // no obligation for this one
        Foo f1 = new Foo();
        // obligation for this one
        // :: error: required.method.not.called
        Foo f2 = new Foo("hi");
    }
}
