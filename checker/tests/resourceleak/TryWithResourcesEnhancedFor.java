// An enhanced for loop, inside a try-with-resources statement, that iterates over the resource.
// The resource's iterator is a @MustCallAlias of the resource.

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;

class TryWithResourcesEnhancedFor {

  abstract static class LineReader extends BufferedReader
      implements Iterable<String>, Iterator<String> {

    @MustCallAlias LineReader(@MustCallAlias Reader r) {
      super(r);
    }

    @Override
    @SuppressWarnings("mustcall:override.return")
    public @MustCallAlias Iterator<String> iterator(@MustCallAlias LineReader this) {
      return this;
    }
  }

  static native @MustCallAlias LineReader makeLineReader(@MustCallAlias Reader r);

  static class CheckedException extends IOException {}

  static native void mightThrow() throws CheckedException;

  void test(File file) throws IOException {
    try (LineReader lr = makeLineReader(new FileReader(file))) {
      for (String line : lr) {
        mightThrow();
      }
    }
  }
}
