// Test case for a WPI crash caused by mismatches between captured type variables
// and the declared type of a field: in particular, the issue is that base.next()
// the next field actually have slightly different types: base.next()'s type is
// a capture that extends T.

import java.util.Iterator;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

@SuppressWarnings("purity.incorrect.sideeffectsonly") // not relevant to this test case
public class Dataset6Crash {

  public static <T> Iterator<T> limit(
      final Iterator<? extends T> base, final CountingPredicate<? super T> filter) {
    return new Iterator<T>() {

      private T next;

      private boolean end;

      private int index = 0;

      public boolean hasNext() {
        return true;
      }

      @SideEffectsOnly("this")
      public T next() {
        fetch();
        T r = next;
        next = null;
        return r;
      }

      @SideEffectsOnly({"this"}) // adding "base" leads to "identifier not found" error
      private void fetch() {
        if (next == null && !end) {
          if (base.hasNext()) {
            next = base.next();
            if (!filter.apply(index++, next)) {
              next = null;
              end = true;
            }
          } else {
            end = true;
          }
        }
      }

      @SideEffectsOnly("this")
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  private static class CountingPredicate<T> {
    @Pure
    public boolean apply(int i, T next) {
      return false;
    }
  }
}
