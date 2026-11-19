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

  // Neither `RawIterator` nor `Iterator<String>` is a subtype of the other, but `RawIterator` is
  // return-type-substitutable for `Iterator<String>` by an unchecked conversion, and not vice
  // versa.  So javac chooses `PureRawIterable.iterator()`, regardless of the order of the
  // interfaces.
  @SuppressWarnings("rawtypes")
  interface RawIterator extends Iterator {}

  interface PureRawIterable {
    @SideEffectFree
    RawIterator iterator();
  }

  interface PureRawIterableFirst extends PureRawIterable, PlainIterable {}

  interface PureRawIterableLast extends PlainIterable, PureRawIterable {}

  void pureRawIterableFirst(PureRawIterableFirst r) {
    RawIterator itor = r.iterator();
    if (f != null) {
      for (String s : r) {
        f.toString();
      }
      f.toString();
    }
  }

  void pureRawIterableLast(PureRawIterableLast r) {
    RawIterator itor = r.iterator();
    if (f != null) {
      for (String s : r) {
        f.toString();
      }
      f.toString();
    }
  }

  // A type may inherit a generic `iterator()` method.  Its return type is the same as that of
  // `Iterable<String>.iterator()`, so javac may choose either method.  `AGenericIterable` sorts
  // before `PlainIterable`, so the generic method may come first.
  interface AGenericIterable {
    <T> Iterator<String> iterator();
  }

  interface GenericIterableFirst extends AGenericIterable, PlainIterable {}

  interface GenericIterableLast extends PlainIterable, AGenericIterable {}

  void genericIterableFirst(GenericIterableFirst r) {
    for (String s : r) {
      s.toString();
    }
  }

  void genericIterableLast(GenericIterableLast r) {
    for (String s : r) {
      s.toString();
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
