// A simple test that the extra obligations that MustCallAlias imposes are
// respected. This version gets it wrong by assigning the MCA param to a non-owning
// field.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

public class MustCallAliasImplWrong2 implements Closeable {

  final /*@Owning*/ Closeable foo;

  // :: error: (mustcallalias.out.of.scope)
  public @MustCallAlias MustCallAliasImplWrong2(@MustCallAlias Closeable foo) {
    // The following error isn't really desirable, but occurs because the special case
    // in the Must Call Checker for assigning @MustCallAlias parameters to @Owning fields
    // is not triggered.
    // :: error: (assignment)
    this.foo = foo;
  }

  @Override
  public void close() {}
}
