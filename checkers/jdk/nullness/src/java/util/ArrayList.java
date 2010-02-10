package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// permits null elements
public class ArrayList<E extends @Nullable Object> extends java.util.AbstractList<E> implements java.util.List<E>, java.util.RandomAccess, java.lang.Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 8683452581122892189L;
  public ArrayList(int a1) { throw new RuntimeException("skeleton method"); }
  public ArrayList() { throw new RuntimeException("skeleton method"); }
  public ArrayList(java.util.Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public void trimToSize() { throw new RuntimeException("skeleton method"); }
  public void ensureCapacity(int a1) { throw new RuntimeException("skeleton method"); }
  public int size() { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public boolean contains(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public int indexOf(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public <T> @Nullable T [] toArray(T[] a1) { throw new RuntimeException("skeleton method"); }
  public @Pure E get(int a1) { throw new RuntimeException("skeleton method"); }
  public E set(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public E remove(int a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public boolean addAll(java.util.Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(int a1, java.util.Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
  public Object clone() { throw new RuntimeException("skeleton method"); }
}
