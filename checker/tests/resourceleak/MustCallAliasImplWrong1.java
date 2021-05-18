// A simple test that the extra obligations that MustCallAlias imposes are
// respected. This version gets it wrong by not assigning the MCA param
// to a field.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

public class MustCallAliasImplWrong1 implements Closeable {

  final @Owning Closeable foo;

  // :: error: required.method.not.called
  public @MustCallAlias MustCallAliasImplWrong1(@MustCallAlias Closeable foo) {
    this.foo = null;
  }

  @Override
  @EnsuresCalledMethods(
      value = {"this.foo"},
      methods = {"close"})
  public void close() throws IOException {
    this.foo.close();
  }
}
