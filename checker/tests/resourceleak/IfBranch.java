// Tests for cases where an interesting state merge has to happen after an if-branch.

import java.io.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.nullness.qual.*;

abstract class IfBranch {

  public abstract boolean choice();

  public abstract @Owning Closeable alloc() throws IOException;

  // False positive that confuses the consistency analyzer:
  //  - The then-branch makes r1 and result resource aliases.
  //  - The else-branch closes r1.
  //  - When control flow merges, we end up with
  //    + An obligation to close r1 (from the then-branch)
  //    + r1 has no called methods (from the else-branch)
  //    + r1 and result are not necessarily aliases
  //    resulting in a spurious violation on return.
  // :: error: (required.method.not.called)
  public @Owning @Nullable Closeable test1(@Owning Closeable r1) throws IOException {
    Closeable result;
    if (choice()) {
      result = r1;
    } else {
      r1.close();
      result = null;
    }
    return result;
  }

  // Variant of test1 using multiple returns instead of a result variable.  This one works.
  public @Owning @Nullable Closeable test2(@Owning Closeable r1) throws IOException {
    if (choice()) {
      return r1;
    } else {
      r1.close();
      return null;
    }
  }
}
