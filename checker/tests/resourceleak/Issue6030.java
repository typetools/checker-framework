// Test case for https://github.com/typetools/checker-framework/issues/6030

import java.util.*;
import org.checkerframework.checker.mustcall.qual.Owning;

public class Issue6030 {

  interface CloseableIterator<T> extends Iterator<T>, java.io.Closeable {}

  static class MyScanner<T, I extends MyScanner<T, I>> implements CloseableIterator<T> {
    @Owning I iterator;

    // :: error: missing.creates.mustcall.for
    public boolean hasNext() {
      if (iterator == null) iterator = createIterator();
      return iterator.hasNext();
    }

    public T next() {
      return null;
    }

    private I createIterator() {
      return null;
    }

    public void close() {}
  }
}
