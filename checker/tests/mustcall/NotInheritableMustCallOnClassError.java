// A test that checks that writing @MustCall instead of @InheritableMustCall
// on a class declaration generates an error, as discussed in
// https://github.com/typetools/checker-framework/issues/5181.

import org.checkerframework.checker.mustcall.qual.*;

@MustCall("foo")
// :: warning: mustcall.not.inheritable
public class NotInheritableMustCallOnClassError {
    public void foo() {}
}
