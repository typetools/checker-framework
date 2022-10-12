// A test that ensures that the Resource Leak Checker continues to track an
// Object if it's returned from a method with a non-empty @MustCall annotation.

import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;

public class ReturnOwningObject {
    @InheritableMustCall("a")
    static class Foo {
        void a() {}
    }

    // This is unsatisfiable without a cast, but
    // for soundness the RLC still needs to track it.
    public static @MustCall("a") Object getFoo() {
        return new Foo();
    }

    public static void useGetFoo() {
        // :: error: required.method.not.called
        Object obj = getFoo();
    }
}
