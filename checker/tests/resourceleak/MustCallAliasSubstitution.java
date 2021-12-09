// A test case that checks that resolving the obligations of one object and then
// substituting a fresh object is not counted as an alias, for the purpose of MustCallAlias
// verification.

import java.io.*;
import java.net.Socket;
import org.checkerframework.checker.mustcall.qual.*;

class MustCallAliasSubstitution {

  // :: error: mustcallalias.out.of.scope
  static @MustCallAlias Closeable example(@MustCallAlias Closeable p) throws IOException {
    p.close();
    return new Socket("localhost", 5000);
  }
}
