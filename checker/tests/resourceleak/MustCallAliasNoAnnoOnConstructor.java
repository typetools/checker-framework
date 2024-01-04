// Test case for https://github.com/typetools/checker-framework/issues/6376

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("close")
public class MustCallAliasNoAnnoOnConstructor {

  final @Owning InputStream is;

  // :: warning: (mustcallalias.method.return.and.param)
  @MustCallAlias MustCallAliasNoAnnoOnConstructor(InputStream p, boolean b) throws Exception {
    if (b) {
      throw new Exception("an exception!");
    }
    this.is = p;
  }

  @EnsuresCalledMethods(value = "this.is", methods = "close")
  public void close() throws IOException {
    this.is.close();
  }
}
