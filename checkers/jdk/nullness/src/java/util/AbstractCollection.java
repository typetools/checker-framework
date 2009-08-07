package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class AbstractCollection<E extends @NonNull Object> implements java.util.Collection<E> {
  public abstract java.util.Iterator<E> iterator();
  public abstract int size();
  public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public boolean contains(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public <T> @Nullable T [] toArray(@Nullable T [] a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsAll(java.util.Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(java.util.Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public boolean removeAll(java.util.Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public boolean retainAll(java.util.Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
}
