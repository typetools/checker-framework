package java.util;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import java.util.Collection;
import java.util.stream.Stream;

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class AbstractCollection<E> implements Collection<E> {
  protected AbstractCollection() {}
  @Override
  @SideEffectFree public abstract Iterator<E> iterator();
  @Override
  @Pure public abstract int size();
  @Override
  @Pure public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  @Override
  @Pure public boolean contains(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Override
  @SideEffectFree public Object [] toArray() { throw new RuntimeException("skeleton method"); }
  @Override
  @SideEffectFree public <T> @Nullable T @PolyNull [] toArray(@Nullable T @PolyNull [] a1) { throw new RuntimeException("skeleton method"); }
  @Override
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  @Override
  public boolean remove(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Override
  @Pure public boolean containsAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  @Override
  public boolean addAll(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  @Override
  public boolean removeAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  @Override
  public boolean retainAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  @Override
  public void clear() { throw new RuntimeException("skeleton method"); }
  @Override
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
}
