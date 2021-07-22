// A test that a class can have multiple MCA constructors.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;

class MustCallAliasPassthroughThis extends FilterInputStream {
    @MustCallAlias MustCallAliasPassthroughThis(@MustCallAlias InputStream is) {
        super(is);
    }

    @MustCallAlias MustCallAliasPassthroughThis(@MustCallAlias InputStream is, int x) {
        this(is);
    }
}
