// This is a test for what happens when there's a missing MCA return type.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class MustCallAliasPassthroughWrong3 {
  // Both of these verify - it's okay to leave off the MCA param, because the return type is
  // owning - but the first one leads to imprecision at call sites.
  static InputStream missingMCA(@MustCallAlias InputStream is) {
    return is;
  }

  static @MustCallAlias InputStream withMCA(@MustCallAlias InputStream is) {
    return is;
  }

  // :: error: required.method.not.called
  void use_bad(@Owning InputStream is) throws Exception {
    InputStream is2 = missingMCA(is);
    is2.close();
  }

  void use_good(@Owning InputStream is) throws Exception {
    InputStream is2 = withMCA(is);
    is2.close();
  }
}
