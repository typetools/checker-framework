// A test case that the RLC's -AnoCreatesMustCallFor argument doesn't change
// warning suppression behavior for the Must Call Checker.

import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

@SuppressWarnings("required.method.not.called")
class DifferentSWKeys {
    void test(@Owning @MustCall("foo") Object obj) {
        // :: warning: unneeded.suppression
        @SuppressWarnings("mustcall")
        @MustCall("foo") Object bar = obj;
    }

    void test2(@Owning @MustCall("foo") Object obj) {
        // actually needed suppression
        @SuppressWarnings("mustcall")
        @MustCall({}) Object bar = obj;
    }

    void test3(@Owning @MustCall("foo") Object obj) {
        // test that the option-specific suppression key doesn't work
        @SuppressWarnings("mustcallnocreatesmustcallfor")
        // :: error: assignment.type.incompatible
        @MustCall({}) Object bar = obj;
    }
}
