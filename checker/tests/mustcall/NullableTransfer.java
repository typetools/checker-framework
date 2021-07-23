// A test that the must-call type of an object tested against null
// is always empty.

import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;

class NullableTransfer {

    void test(@Owning InputStream is) {
        if (is == null) {
            @MustCall({}) InputStream is2 = is;
        } else {
            // :: error: assignment.type.incompatible
            @MustCall({}) InputStream is3 = is;
            @MustCall("close") InputStream is4 = is;
        }
    }
}
