package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class AbstractCollection<E extends @Nullable Object> implements Collection<E> {
  protected AbstractCollection() {}
  public abstract Iterator<E> iterator();
  public abstract int size();
  public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public boolean contains(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public <T> @Nullable T @PolyNull [] toArray(@Nullable T @PolyNull [] a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public boolean removeAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public boolean retainAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
}
