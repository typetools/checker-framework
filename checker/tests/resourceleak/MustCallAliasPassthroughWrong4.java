// A test that a class can extend another class with an MCA constructor,
// and have its own constructor be MCA as well.
// This version just closes the MCA parameter, which isn't wrong so much as weird but I wanted a
// test for it. Issuing an error here is appropriate, because no aliasing relationship
// actually exists after the constructor returns.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;

class MustCallAliasPassthroughWrong4 extends FilterInputStream {
    // :: error: mustcallalias.out.of.scope
    @MustCallAlias MustCallAliasPassthroughWrong4(@MustCallAlias InputStream is) throws Exception {
        super(null);
        is.close();
    }
}
