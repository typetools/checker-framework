// A test of the stores that the consistency check uses for the CFG edge from the entry block, whose
// successor is a loop header when the method body begins with a loop.  The loop header's input
// store merges the store for the edge from the entry block with the store for the loop's back
// edge.

import java.io.Closeable;
import java.io.IOException;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

class MustCallStoreForEntryEdge {

  // :: error: (required.method.not.called)
  void leakLoopAtEntry(@Owning Closeable c, boolean b) {
    while (b) {}
  }

  void closeLoopAtEntry(@Owning Closeable c, boolean b) throws IOException {
    while (b) {}
    c.close();
  }

  // :: error: (required.method.not.called)
  <T extends @MustCall("close") Object> void leakTypeVarLoopAtEntry(@Owning T t, boolean b) {
    while (b) {}
  }

  <T extends Object> void noObligationTypeVarLoopAtEntry(@Owning T t, boolean b) {
    while (b) {}
  }

  // :: error: (required.method.not.called)
  void leakEmptyBody(@Owning Closeable c) {}

  // :: error: (required.method.not.called)
  void leakDoWhileAtEntry(@Owning Closeable c, boolean b) {
    do {
      c = null;
    } while (b);
  }
}
