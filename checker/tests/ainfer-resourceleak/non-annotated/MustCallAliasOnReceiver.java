// @skip-test until we have support for adding annotation on the receiver parameter.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("close")
public class MustCallAliasOnReceiver {

  final @Owning InputStream is;

  @MustCallAlias MustCallAliasOnReceiver(@MustCallAlias InputStream p, boolean b) {
    this.is = p;
  }

  MustCallAliasOnReceiver returnReceiver(MustCallAliasOnReceiver this) {
    return this;
  }

  // :: warning: (required.method.not.called)
  void testReceiverMCAAnnotation(@Owning InputStream inputStream) throws IOException {
    MustCallAliasOnReceiver mcar = new MustCallAliasOnReceiver(is, false);
    mcar.returnReceiver().close();
  }

  @EnsuresCalledMethods(value = "this.is", methods = "close")
  public void close() throws IOException {
    this.is.close();
  }

  public static MustCallAliasOnReceiver mcaneFactory(InputStream is) {
    return new MustCallAliasOnReceiver(is, false);
  }

  // :: warning: (required.method.not.called)
  public static void testUse(@Owning InputStream inputStream) throws Exception {
    MustCallAliasOnReceiver mcane = mcaneFactory(inputStream);
    mcane.close();
  }
}
