// A test that a class can extend another class with an MCA constructor,
// and have its own constructor be MCA as well.
// This version passes the MCA param to another method instead of the passthrough constructor.
// This is sort of okay - the stream does get closed, if it needs to be closed - though the
// MCA annotation on the return type is super misleading and will lead to FPs. It would be better
// to annotate code like this with @Owning on the constructor.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class MustCallAliasPassthroughWrong2 extends FilterInputStream {
  // :: error: (mustcallalias.out.of.scope)
  @MustCallAlias MustCallAliasPassthroughWrong2(@MustCallAlias InputStream is) throws Exception {
    super(null);
    // The following error isn't really desirable, but occurs because the special case
    // in the Must Call Checker for assigning @MustCallAlias parameters to @Owning fields
    // is not triggered, and @MustCallAlias is treated as @PolyMustCall otherwise.
    // :: error: argument
    closeIS(is);
  }

  void closeIS(@Owning InputStream is) throws Exception {
    is.close();
  }
}
