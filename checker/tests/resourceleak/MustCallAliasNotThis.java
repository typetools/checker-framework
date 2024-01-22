// A test case with examples of code that shouldn't pass the checker where an @MustCallAlias
// parameter is passed to an owning field, but not an owning field of this.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

public class MustCallAliasNotThis implements Closeable {

  @Owning Closeable foo;

  // Both of these constructors are wrong: the first assigns to the owning field of another
  // object of the same class, but not the "this" object; the second assigns to another class'
  // owning field entirely. Both of these assignments would require @CreatesMustCallFor annotations
  // to verify, so it's okay that the @MustCallAlias annotations are verified here (because it
  // is impossible to write the required @CreatesMustCallFor annotations, since they can only be
  // written on methods, not on constructors). If we ever permit @CreatesMustCallFor annotations
  // on constructors, this test should be revisited: it might be necessary to make a corresponding
  // change in the rules for verifying @MustCallAlias.

  // :: error: missing.creates.mustcall.for
  public @MustCallAlias MustCallAliasNotThis(
      @MustCallAlias Closeable foo, MustCallAliasNotThis other) throws IOException {
    other.close();
    other.foo = foo;
  }

  // :: error: missing.creates.mustcall.for
  public @MustCallAlias MustCallAliasNotThis(@MustCallAlias Closeable foo, Bar other)
      throws IOException {
    other.close();
    other.baz = foo;
  }

  @Override
  @EnsuresCalledMethods(
      value = {"this.foo"},
      methods = {"close"})
  public void close() throws IOException {
    this.foo.close();
  }

  class Bar implements Closeable {
    @Owning Closeable baz;

    @Override
    @EnsuresCalledMethods(
        value = {"this.baz"},
        methods = {"close"})
    public void close() throws IOException {
      this.baz.close();
    }
  }
}
