// A simple test that the extra obligations that MustCallAlias imposes are
// respected.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

public class MustCallAliasImpl implements Closeable {

  final @Owning Closeable foo;

  // I got this error here: (type.invalid.annotations.on.use)
  public @MustCallAlias MustCallAliasImpl(@MustCallAlias Closeable foo) {
    this.foo = foo;
  }

  @Override
  @EnsuresCalledMethods(
      value = {"this.foo"},
      methods = {"close"})
  public void close() throws IOException {
    this.foo.close();
  }
}
