// Test case for https://github.com/typetools/checker-framework/issues/5597

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("close")
public class MustCallAliasMethodReturnAndParam {

  final @Owning InputStream is;

  // No error on a constructor.
  MustCallAliasMethodReturnAndParam(@MustCallAlias InputStream p, boolean b) throws Exception {
    if (b) {
      throw new Exception("an exception!");
    }
    this.is = p;
  }

  @EnsuresCalledMethods(value = "this.is", methods = "close")
  public void close() throws IOException {
    this.is.close();
  }

  // :: warning: (mustcallalias.method.return.and.param)
  public static MustCallAliasMethodReturnAndParam mcaneFactory(@MustCallAlias InputStream is)
      throws Exception {
    return new MustCallAliasMethodReturnAndParam(is, false);
  }
}
