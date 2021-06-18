// A test that a class can have multiple MCA constructors.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class MustCallAliasPassthroughThis extends FilterInputStream {
  @MustCallAlias MustCallAliasPassthroughThis(@MustCallAlias InputStream is) {
    super(is);
  }

  @MustCallAlias MustCallAliasPassthroughThis(@MustCallAlias InputStream is, int x) {
    this(is);
  }
}
