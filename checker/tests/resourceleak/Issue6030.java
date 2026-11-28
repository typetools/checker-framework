// Test case for https://github.com/typetools/checker-framework/issues/6030

import java.util.*;
import org.checkerframework.checker.collectionownership.qual.NotOwningCollection;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class Issue6030 {

  interface CloseableIterator<T> extends Iterator<T>, java.io.Closeable {}

  static class MyScanner<T, I extends MyScanner<T, I>> implements CloseableIterator<T> {
    @Owning I iterator;

    // :: error: missing.creates.mustcall.for
    public boolean hasNext() {
      if (iterator == null) iterator = createIterator();
      return iterator.hasNext();
    }

    // Since #7602, Iterator#next returns @NotOwning.
    @SideEffectsOnly("this")
    public @NotOwning T next() {
      return null;
    }

    // Since #7602, Iterator#hasNext has a @NotOwningCollection receiver, and hasNext() calls this.
    private I createIterator(@NotOwningCollection MyScanner<T, I> this) {
      return null;
    }

    public void close() {}
  }
}
