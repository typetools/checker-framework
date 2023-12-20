// Test case for https://github.com/typetools/checker-framework/issues/5597

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("close")
public class MustCallAliasReturnAndParamSimple {

  final @Owning InputStream is;

  @MustCallAlias MustCallAliasReturnAndParamSimple(@MustCallAlias InputStream p, boolean b) throws Exception {
    if (b) {
      throw new Exception("an exception!");
    }
    this.is = p;
  }

  @EnsuresCalledMethods(value = "this.is", methods = "close")
  public void close() throws IOException {
    this.is.close();
  }

  // No errors or warnings here, properly annotated.
  public static @MustCallAlias MustCallAliasReturnAndParamSimple mcaneFactory(
      @MustCallAlias InputStream is) throws Exception {
    return new MustCallAliasReturnAndParamSimple(is, false);
  }

  // :: warning: (mustcallalias.method.return.and.param)
  public static MustCallAliasReturnAndParamSimple mcaneFactory2(@MustCallAlias InputStream is)
      throws Exception {
    // :: error: (return)
    return new MustCallAliasReturnAndParamSimple(is, false);
  }

  // :: warning: (mustcallalias.method.return.and.param)
  public static @MustCallAlias MustCallAliasReturnAndParamSimple mcaneFactory3(InputStream is)
      throws Exception {
    return new MustCallAliasReturnAndParamSimple(is, false);
  }

  // No warning here; neither the return type or parameter is annotated with @MustCallAlias
  public static MustCallAliasReturnAndParamSimple mcaneFactory4(InputStream is) throws Exception {
    return new MustCallAliasReturnAndParamSimple(is, false);
  }
}
