// A simple test that the extra obligations that MustCallAlias imposes are
// respected. Identical to MustCallAliasImpl.java except the @Owning annotation
// on foo has been removed, making this unverifiable.

// @skip-test until the checks are implemented

import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;

public class MustCallAliasImplNoOwning implements Closeable {

    final Closeable foo;

    // :: error: mustcall.alias.invalid
    public @MustCallAlias MustCallAliasImplNoOwning(@MustCallAlias Closeable foo) {
        this.foo = foo;
    }

    @Override
    public void close() throws IOException {
        this.foo.close();
    }
}
