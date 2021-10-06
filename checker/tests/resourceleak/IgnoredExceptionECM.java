// A test that the Resource Leak Checker ignores exceptions in destructors the same way that it
// does in the consistency checker.

import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("foo") class IgnoredExceptionECM {

    @Owning
    @MustCall("toString") Object obj;

    @EnsuresCalledMethods(value = "this.obj", methods = "toString")
    void foo() {
        // This line will produce an exception,
        // which the RLC should ignore and verify the method.
        int y = 5 / 0;
        this.obj.toString();
    }
}
