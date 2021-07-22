// A simple test that the extra obligations that MustCallAlias imposes are
// respected.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;

public class MustCallAliasImpl implements Closeable {

    final @Owning Closeable foo;

    public @MustCallAlias MustCallAliasImpl(@MustCallAlias Closeable foo) {
        this.foo = foo;
    }

    @Override
    @EnsuresCalledMethods(
            value = {"this.foo"},
            methods = {"close"})
    public void close() throws IOException {
        this.foo.close();
    }
}
