// A test of the Must Call store that the consistency check uses for a CFG edge from a block that
// has no nodes, such as a conditional block, to the exit block.  The iterator variable is a
// resource alias of the resource, and it is unassigned on the path through the early return, so
// the exit block's input store, which merges every path to the exit, has no value for it.  The
// store for the edge does have a value for it.

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;

class MustCallStoreForEdge {

  static class LineReader extends BufferedReader implements Iterable<String>, Iterator<String> {
    @MustCallAlias LineReader(@MustCallAlias Reader in) {
      super(in);
    }

    LineReader(String filename) throws IOException {
      this(new FileReader(filename));
    }

    @Override
    @SuppressWarnings("mustcall:override.return") // the iterator is this, which must be closed
    public @MustCallAlias Iterator<String> iterator(@MustCallAlias LineReader this) {
      return this;
    }

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public String next() {
      throw new Error();
    }
  }

  void leakWhileLoop(String filename, boolean b, boolean c) throws IOException {
    // :: error: (required.method.not.called)
    LineReader lr = new LineReader(filename);
    if (b) {
      lr.close();
      return;
    }
    Iterator<String> it = lr.iterator();
    while (c) {}
  }

  void leakIfReturn(String filename, boolean b, boolean c) throws IOException {
    // :: error: (required.method.not.called)
    LineReader lr = new LineReader(filename);
    if (b) {
      lr.close();
      return;
    }
    Iterator<String> it = lr.iterator();
    if (c) {
      return;
    }
  }

  // The resource leaks only along the then edge of the second conditional.
  void leakOnThenEdge(String filename, boolean b, boolean c) throws IOException {
    // :: error: (required.method.not.called)
    LineReader lr = new LineReader(filename);
    if (b) {
      lr.close();
      return;
    }
    Iterator<String> it = lr.iterator();
    if (c) {
      return;
    }
    lr.close();
  }
}
