import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("close")
public class MustCallAliasOnReceiver {

  final @Owning InputStream is;

  @MustCallAlias MustCallAliasOnReceiver(@MustCallAlias InputStream p, boolean b) {
    this.is = p;
  }

  @EnsuresCalledMethods(value = "this.is", methods = "close")
  public void close() throws IOException {
    this.is.close();
  }

  public static MustCallAliasOnReceiver mcaneFactory(InputStream is) {
    return new MustCallAliasOnReceiver(is, false);
  }

  // :: error: required.method.not.called
  public static void testUse3(@Owning InputStream inputStream) throws Exception {
    // if mcaneFactory throws, then inputStream goes out of scope w/o being closed
    MustCallAliasOnReceiver mcane = mcaneFactory(inputStream);
    mcane.close();
  }
}
