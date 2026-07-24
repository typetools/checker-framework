// Test case for https://github.com/typetools/checker-framework/issues/6030

import java.util.*;
import org.checkerframework.checker.collectionownership.qual.NotOwningCollection;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.mustcall.qual.Owning;

public class Issue6030 {

  interface CloseableIterator<T> extends Iterator<T>, java.io.Closeable {}

  static class MyScanner<T, I extends MyScanner<T, I>> implements CloseableIterator<T> {
    @Owning I iterator;

    // :: error: missing.creates.mustcall.for
    public boolean hasNext(@NotOwningCollection MyScanner<T, I> this) {
      if (iterator == null) iterator = createIterator();
      return iterator.hasNext();
    }

    // The @NotOwning annotation is required to be consistent with the superclass implementation.
    // The return type of Iterator#next is @NotOwning. Soundness is ensured by the RLC for
    // collections.
    public @NotOwning T next(@NotOwningCollection MyScanner<T, I> this) {
      return null;
    }

    private I createIterator(@NotOwningCollection MyScanner<T, I> this) {
      return null;
    }

    public void close() {}
  }
}
