// A test that checks that writing @MustCall instead of @InheritableMustCall
// on a class declaration does not generate an error when the class is final.

import org.checkerframework.checker.mustcall.qual.*;

@MustCall("foo") public final class NotInheritableMustCallOnFinalClassNoError {
    public void foo() {}
}
