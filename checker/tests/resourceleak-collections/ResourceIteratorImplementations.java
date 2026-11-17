// An implementation of Iterator whose elements are resources must annotate its next() method
// @NotOwning, to match Iterator.next().  An implementation whose elements have no must-call
// obligations need not; see ResourceFreeCollectionImplementations.java.

import java.util.Iterator;
import org.checkerframework.checker.mustcall.qual.NotOwning;

class ResourceIteratorImplementations {

  abstract static class OwningNext implements Iterator<Resource> {
    @Override
    // :: error: (owning.override.return)
    public Resource next() {
      return new Resource();
    }
  }

  abstract static class NotOwningNext implements Iterator<Resource> {
    @Override
    public @NotOwning Resource next() {
      throw new Error();
    }
  }

  // The upper bound of T has no must-call obligations, so T is never a resource type.
  abstract static class GenericNext<T> implements Iterator<T> {
    @Override
    public T next() {
      throw new Error();
    }
  }
}
