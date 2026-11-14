// The desugared `iterator()` call of an enhanced `for` loop invokes the method that javac's method
// resolution chooses.  A type that inherits `iterator()` from two unrelated interfaces invokes the
// one with the more specific return type, `PureIterable.iterator()`, regardless of the order of the
// interfaces.  That method is `@SideEffectFree`, so the loop does not invalidate the refinement of
// `f`.  (The loop's `hasNext()` and `next()` calls are those of `Iterator`, which do not modify
// `f`.)

import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class EnhancedForIteratorResolution {

  @Nullable Object f;

  interface PureIterator extends Iterator<String> {}

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

  // `PlainIterableClass.iterator()` is abstract, so it does not take precedence over
  // `PureIterable.iterator()`.  Of the two abstract methods, javac chooses the one with the more
  // specific return type.
  abstract static class PlainIterableClass implements Iterable<String> {
    @Override
    public abstract Iterator<String> iterator();
  }

  abstract static class PureIterableSubclass extends PlainIterableClass implements PureIterable {}

  void pureIterableSubclass(PureIterableSubclass r) {
    if (f != null) {
      for (String s : r) {
        f.toString();
      }
      f.toString();
    }
  }

  // A concrete method of a class takes precedence over an abstract method of an interface.
  interface SideEffectFreeIterable {
    @SideEffectFree
    Iterator<String> iterator();
  }

  static class ConcretePlainIterableClass implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
      throw new UnsupportedOperationException();
    }
  }

  abstract static class ConcretePlainIterableSubclass extends ConcretePlainIterableClass
      implements SideEffectFreeIterable {}

  void concretePlainIterableSubclass(ConcretePlainIterableSubclass r) {
    if (f != null) {
      for (String s : r) {
        // :: error: [dereference.of.nullable]
        f.toString();
      }
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
