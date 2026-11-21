// Test case for an enhanced for loop over a resource whose iterator() method returns
// @MustCallAlias, inside a try-with-resources statement whose body may throw.

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Iterator;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;

public class EnhancedForMustCallAlias extends LineNumberReader
    implements Iterable<String>, Iterator<String> {

  public @MustCallAlias EnhancedForMustCallAlias(@MustCallAlias Reader r) {
    super(r);
  }

  @SuppressWarnings("mustcall:override.return") // Iterable.iterator() returns @MustCall({})
  @Override
  public @MustCallAlias Iterator<String> iterator(@MustCallAlias EnhancedForMustCallAlias this) {
    return this;
  }

  @Override
  public boolean hasNext() {
    return false;
  }

  @Override
  public String next() {
    return "";
  }

  static void mayThrow() throws IOException {}

  static void loopFirst(String filename) throws IOException {
    try (EnhancedForMustCallAlias r = new EnhancedForMustCallAlias(new FileReader(filename))) {
      for (String line : r) {
        mayThrow();
      }
    }
  }

  static void loopNotFirst(String filename) throws IOException {
    try (EnhancedForMustCallAlias r = new EnhancedForMustCallAlias(new FileReader(filename))) {
      mayThrow();
      for (String line : r) {
        mayThrow();
      }
    }
  }

  static void leak(String filename) throws IOException {
    // :: error: [required.method.not.called]
    EnhancedForMustCallAlias r = new EnhancedForMustCallAlias(new FileReader(filename));
    for (String line : r) {
      mayThrow();
    }
  }

  static void leakOnException(String filename) throws IOException {
    // :: error: [required.method.not.called]
    EnhancedForMustCallAlias r = new EnhancedForMustCallAlias(new FileReader(filename));
    for (String line : r) {
      mayThrow();
    }
    r.close();
  }
}
