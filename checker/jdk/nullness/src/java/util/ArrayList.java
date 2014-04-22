package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

// permits null elements
public class ArrayList<E extends @Nullable Object> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 8683452581122892189L;
  public ArrayList(int a1) { throw new RuntimeException("skeleton method"); }
  public ArrayList() { throw new RuntimeException("skeleton method"); }
  public ArrayList(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public void trimToSize() { throw new RuntimeException("skeleton method"); }
  public void ensureCapacity(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int size() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean contains(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int indexOf(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int lastIndexOf(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public <T> @Nullable T @PolyNull [] toArray(T @PolyNull [] a1) { throw new RuntimeException("skeleton method"); }
  @Pure public E get(int a1) { throw new RuntimeException("skeleton method"); }
  public E set(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public E remove(int a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public boolean addAll(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(int a1, Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Object clone() { throw new RuntimeException("skeleton method"); }
}
