// Test case for https://github.com/typetools/checker-framework/issues/5597

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("close")
public class MustCallAliasNormalExit {

  final @Owning InputStream is;

  @MustCallAlias MustCallAliasNormalExit(@MustCallAlias InputStream p, boolean b) throws Exception {
    if (b) {
      throw new Exception("an exception!");
    }
    this.is = p;
  }

  @EnsuresCalledMethods(value = "this.is", methods = "close")
  public void close() throws IOException {
    this.is.close();
  }

  public static @MustCallAlias MustCallAliasNormalExit mcaneFactory(InputStream is)
      throws Exception {
    return new MustCallAliasNormalExit(is, false);
  }

  // :: error: required.method.not.called
  public static void testUse1(@Owning InputStream inputStream) throws IOException {
    MustCallAliasNormalExit mcane = null;
    try {
      mcane = new MustCallAliasNormalExit(inputStream, true); // at run time, this WILL throw
    } catch (Exception e) {
      // At run time would fail (NPE), but for illustrative purposes this is fine.
      // This absolutely must not cause inputStream to be considered closed because of the
      // MustCallAlias
      // relationship.
      mcane.close();
    }
  }

  // :: error: required.method.not.called
  public static void testUse2(@Owning InputStream inputStream) throws IOException {
    MustCallAliasNormalExit mcane = null;
    try {
      mcane = mcaneFactory(inputStream);
    } catch (Exception e) {
      mcane.close();
    }
  }

  // :: error: required.method.not.called
  public static void testUse3(@Owning InputStream inputStream) throws Exception {
    // if mcaneFactory throws, then inputStream goes out of scope w/o being closed
    MustCallAliasNormalExit mcane = mcaneFactory(inputStream);
    mcane.close();
  }

  // TODO: this appears to be a false positive, but the RLC doesn't handle it correctly because
  // close() is called on different aliases on different branches.
  // :: error: required.method.not.called
  public static void testUse4(@Owning InputStream inputStream) throws Exception {
    MustCallAliasNormalExit mcane = null;
    try {
      mcane = mcaneFactory(inputStream);
    } catch (Exception e) {
      // this makes it safe
      inputStream.close();
    }
    mcane.close();
  }
}
