// A test that passing a local to an MCA super constructor is allowed.
// This version has been modified to expect errors, as if running under
// -AnoResourceAliases - so @MustCallAlias annotations are ignored.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class MustCallAliasPassthroughLocal extends FilterInputStream {
  MustCallAliasPassthroughLocal(File f) throws Exception {
    // This is safe - this MCA constructor of FilterInputStream means that the result of this
    // constructor - i.e. the caller - is taking ownership of this newly-created output stream.
    // :: error: required.method.not.called
    super(new FileInputStream(f));
  }

  static void test(File f) throws Exception {
    // :: error: required.method.not.called
    new MustCallAliasPassthroughLocal(f);
  }

  static void test_ok(File f) throws Exception {
    new MustCallAliasPassthroughLocal(f).close();
  }
}
