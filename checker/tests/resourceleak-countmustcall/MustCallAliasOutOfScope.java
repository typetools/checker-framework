// Test input for ResourceLeakCountMustCallTest.

import java.io.FilterInputStream;
import java.io.InputStream;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;

class MustCallAliasOutOfScope extends FilterInputStream {
  // The parameter is counted.  Not verified, because the constructor discards it.
  // :: error: [mustcallalias.out.of.scope]
  @MustCallAlias MustCallAliasOutOfScope(@MustCallAlias InputStream is) {
    super(null);
  }
}
