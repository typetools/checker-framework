// A simple test that the extra obligations that MustCallAlias imposes are
// respected. This version gets it wrong by assigning the MCA param to a non-owning
// field.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;

public class MustCallAliasImplWrong2 implements Closeable {

    final /*@Owning*/ Closeable foo;

    // :: error: mustcallalias.out.of.scope
    public @MustCallAlias MustCallAliasImplWrong2(@MustCallAlias Closeable foo) {
        this.foo = foo;
    }

    @Override
    public void close() {}
}
