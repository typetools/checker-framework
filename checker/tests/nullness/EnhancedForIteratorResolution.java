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

  // `AbstractOverrider.iterator()` overrides the concrete `ConcreteOverridden.iterator()`, so the
  // latter is not a member of `OverriderSubclass` and does not take precedence over
  // `APureIterable.iterator()`.  `APureIterable` extends a chain of interfaces so that the closure
  // of `OverriderSubclass`, which lists a type before the types that are closer to `Object`, visits
  // `APureIterable`, then `AbstractOverrider`, then `ConcreteOverridden`.
  interface DerivedIterable extends Iterable<String> {}

  interface DoublyDerivedIterable extends DerivedIterable {}

  interface APureIterable extends DoublyDerivedIterable {
    @Override
    @SideEffectFree
    PureIterator iterator();
  }

  static class ConcreteOverridden implements Iterable<String> {
    @Override
    public PureIterator iterator() {
      throw new UnsupportedOperationException();
    }
  }

  abstract static class AbstractOverrider extends ConcreteOverridden {
    @Override
    @SideEffectFree
    public abstract PureIterator iterator();
  }

  abstract static class OverriderSubclass extends AbstractOverrider implements APureIterable {}

  void overriderSubclass(OverriderSubclass r) {
    if (f != null) {
      for (String s : r) {
        f.toString();
      }
      f.toString();
    }
  }

  // Of two abstract methods with the same return type, javac chooses the one that it finds later.
  // javac searches `APlain` before `ZPure`, because their names are in that order, so it chooses
  // `ZPure.iterator()` regardless of the order of the interfaces.
  interface APlain extends Iterable<String> {
    @Override
    Iterator<String> iterator();
  }

  interface ZPure extends Iterable<String> {
    @Override
    @SideEffectFree
    Iterator<String> iterator();
  }

  interface APlainFirst extends APlain, ZPure {}

  interface ZPureFirst extends ZPure, APlain {}

  void aPlainFirst(APlainFirst r) {
    if (f != null) {
      for (String s : r) {
        f.toString();
      }
      f.toString();
    }
  }

  void zPureFirst(ZPureFirst r) {
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
