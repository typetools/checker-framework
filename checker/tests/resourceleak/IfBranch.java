// Tests for cases where an interesting state merge has to happen after an if-branch.

import java.io.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.nullness.qual.*;

abstract class IfBranch {

  public abstract boolean choice();

  // False positive.  The then-branch moves r1 to result, and the else-branch closes r1.
  // After the if-else block we should be left only with an obligation to close result,
  // which should be satisfied by `return result`.
  //
  // Obligation tracking by the MustCallConsistencyAnalyzer is indeed path sensitive, in that it
  // does not lose information at control-flow merges; it just tracks the obligation(s) from each
  // incoming path separately.  But, local inference of @CalledMethods types in the Called Methods
  // Checker is path insensitive; the @CalledMethods inference computes a least-upper bound at
  // merges using set intersection.  At the program point before the return statement, we get that
  // r1 has type @CalledMethods({}) since no method is called on r1 in the preceding then branch.
  // So, despite tracking the obligation from the else branch separately, we lose the information
  // that close was called on that path and report a false positive.
  //
  // One possible fix would be to stop relying on the local type inference from the Called Methods
  // Checker and instead track called methods for each alias alongside obligations in
  // MustCallConsistencyAnalyzer.
  //
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
