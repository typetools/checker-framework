// A test case that checks that resolving the obligations of one object and then
// substituting a fresh object is not counted as an alias, for the purpose of MustCallAlias
// verification.

import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;
import java.net.Socket;

class MustCallAliasSubstitution {

    // :: error: mustcallalias.out.of.scope
    static @MustCallAlias Closeable example(@MustCallAlias Closeable p) throws IOException {
        p.close();
        return new Socket("localhost", 5000);
    }

    // This method demonstrates how a false negative could occur, if no error was issued
    // on example().
    void use(Closeable c) throws IOException {
        // s never gets closed, but the checker permits this code, because it believes
        // that s and c are aliased.
        Closeable s = example(c);
        c.close();
    }
}
