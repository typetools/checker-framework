// The desugared `iterator()` call of an enhanced `for` loop invokes the method that javac's method
// resolution chooses.  A type that inherits `iterator()` from two unrelated interfaces invokes the
// one with the more specific return type, `PureIterable.iterator()`, regardless of the order of the
// interfaces.  Its iterator's `hasNext()` and `next()` have no side effects, so the loop does not
// invalidate the refinement of `f`.

import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class EnhancedForIteratorResolution {

  @Nullable Object f;

  abstract static class PureIterator implements Iterator<String> {
    @Override
    @Pure
    public abstract boolean hasNext();

    @Override
    @SideEffectFree
    public abstract String next();
  }

  interface PureIterable {
    @SideEffectFree
    PureIterator iterator();
  }

  interface PlainIterable extends Iterable<String> {
    @Override
    Iterator<String> iterator();
  }

  interface PureIterableFirst extends PureIterable, PlainIterable {}

  interface PureIterableLast extends PlainIterable, PureIterable {}

  void pureIterableFirst(PureIterableFirst r) {
    if (f != null) {
      for (String s : r) {
        f.toString();
      }
      f.toString();
    }
  }

  void pureIterableLast(PureIterableLast r) {
    if (f != null) {
      for (String s : r) {
        f.toString();
      }
      f.toString();
    }
  }

  void plainIterable(PlainIterable r) {
    if (f != null) {
      for (String s : r) {
        // :: error: [dereference.of.nullable]
        f.toString();
      }
    }
  }
}
