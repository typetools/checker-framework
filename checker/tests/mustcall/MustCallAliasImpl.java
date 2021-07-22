// A simple test that the extra obligations that MustCallAlias imposes are
// respected.

// @skip-test until the checks are implemented

import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;

public class MustCallAliasImpl implements Closeable {

    @Owning final Closeable foo;

    public @MustCallAlias MustCallAliasImpl(@MustCallAlias Closeable foo) {
        this.foo = foo;
    }

    @Override
    public void close() throws IOException {
        this.foo.close();
    }
}
