// Test case for a set of false positives caused by the Must Call Checker's handling
// of assigning @MustCallAlias parameters to @Owning fields as a special case.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

public class MustCallAliasLocal implements Closeable {

  final @Owning Closeable foo;

  public @MustCallAlias MustCallAliasLocal(@MustCallAlias Closeable foo) {
    Closeable local = foo;
    // The error on the following line is a false positive:
    // :: error: assignment
    this.foo = local;
  }

  @Override
  @EnsuresCalledMethods(
      value = {"this.foo"},
      methods = {"close"})
  public void close() throws IOException {
    this.foo.close();
  }
}
