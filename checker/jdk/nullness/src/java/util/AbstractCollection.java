package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class AbstractCollection<E extends @Nullable Object> implements Collection<E> {
  protected AbstractCollection() {}
  public abstract Iterator<E> iterator();
  @Pure public abstract int size();
  @Pure public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean contains(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public <T> @Nullable T @PolyNull [] toArray(@Nullable T @PolyNull [] a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean containsAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public boolean removeAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public boolean retainAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
}
