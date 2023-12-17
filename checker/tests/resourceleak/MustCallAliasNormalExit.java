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

  public static @MustCallAlias MustCallAliasNormalExit mcaneFactory(@MustCallAlias InputStream is)
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

  public static void testUse3(@Owning InputStream inputStream) throws Exception {
    // If mcaneFactory throws, then inputStream goes out of scope w/o being closed.  But, @Owning
    // only requires that the resource be closed at normal exit, so this is OK.
    MustCallAliasNormalExit mcane = mcaneFactory(inputStream);
    mcane.close();
  }

  // TODO: this is a false positive due to imprecision in our analysis.  At the program point
  // before the call to mcane.close(), the inferred @CalledMethods type of inputStream is
  // @CalledMethods(""), due to the control-flow merge.  Further, this program point may be reached
  // with mcane _not_ being a resource alias of inputStream, if mcaneFactory throws an exception.
  // In this scenario, the analysis reasons that the exit may be reached without close() being
  // called on inputStream.  But, if mcane is not a resource alias of inputStream, then
  // the inputStream.close() call in the catch block must have executed, so this is a false
  // positive.  Removing this false positive would require greater path sensitivity in the
  // consistency analyzer, by tracking both resource aliases and @CalledMethods types for each path
  // in an Obligation.  See https://github.com/typetools/checker-framework/issues/5658
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
