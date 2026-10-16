// A test that a class can extend another class with an MCA constructor,
// and have its own constructor be MCA as well.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class MustCallAliasPassthrough extends FilterInputStream {
  @MustCallAlias MustCallAliasPassthrough(@MustCallAlias InputStream is) {
    super(is);
  }
}
