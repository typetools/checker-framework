// A test of an enhanced for loop over a resource whose iterator() method returns @MustCallAlias.
// The loop's desugared iterator() call might throw an exception before the loop's iterator
// variable is assigned, so the store at a handler that the loop body's exceptions also reach has
// no refined type for the iterator variable.

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;

class MustCallAliasEnhancedFor {

  static class LineReader extends BufferedReader implements Iterable<String>, Iterator<String> {
    @MustCallAlias LineReader(@MustCallAlias Reader in) {
      super(in);
    }

    LineReader(String filename) throws IOException {
      this(new FileReader(filename));
    }

    @Override
    @SuppressWarnings("mustcall:override.return")
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

  static void process(String s) throws IOException {}

  void tryWithResources(String filename) throws IOException {
    try (LineReader lr = new LineReader(filename)) {
      for (String line : lr) {
        process(line);
      }
    }
  }

  void closeInFinally(String filename) throws IOException {
    LineReader lr = new LineReader(filename);
    try {
      for (String line : lr) {
        process(line);
      }
    } finally {
      lr.close();
    }
  }

  void leakInCatch(String filename) throws IOException {
    // :: error: (required.method.not.called)
    LineReader lr = new LineReader(filename);
    try {
      for (String line : lr) {
        process(line);
      }
    } catch (IOException e) {
      return;
    }
    lr.close();
  }
}
