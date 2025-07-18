// @skip-test : contains() has a call to a locally-defined indexOf() method, which is hard to verify

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import org.checkerframework.checker.nonempty.qual.PolyNonEmpty;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class IndexOfNonNegative<E extends Object> extends AbstractSet<E> {

  @SideEffectFree
  public IndexOfNonNegative() {}

  // Query Operations

  @Pure
  @Override
  public int size() {
    return -1;
  }

  @Pure
  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Pure
  private int indexOf(Object value) {
    return -1;
  }

  @Pure
  @Override
  public boolean contains(Object value) {
    // return indexOf(value) != -1;
    if (indexOf(value) != -1) {
      return true;
    } else {
      return false;
    }
  }

  // Modification Operations

  @Override
  public boolean add(E value) {
    return false;
  }

  @Override
  public boolean remove(Object value) {
    return true;
  }

  // Bulk Operations

  @Override
  public boolean addAll(Collection<? extends E> c) {
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return true;
  }

  // Inherit retainAll() from AbstractCollection.

  @Override
  public void clear() {}

  // ///////////////////////////////////////////////////////////////////////////
  // iterators

  @Override
  // :: error: (override.receiver)
  public @PolyNonEmpty Iterator<E> iterator(@PolyNonEmpty IndexOfNonNegative<E> this) {
    throw new Error("");
  }
}
