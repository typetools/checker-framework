// A simple test that MustCallAlias annotations in source code don't issue
// bogus annotations.on.use errors.

import java.io.*;
import org.checkerframework.checker.mustcall.qual.*;

public class MustCallAliasImpl implements Closeable {

  @Owning final Closeable foo;

  public @MustCallAlias MustCallAliasImpl(@MustCallAlias Closeable foo) {
    this.foo = foo;
  }

  @Override
  public void close() throws IOException {
    this.foo.close();
  }
}
