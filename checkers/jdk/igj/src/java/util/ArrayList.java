package java.util;
import checkers.igj.quals.*;

@I
public class ArrayList<E> extends @I java.util.AbstractList<E> implements @I java.util.List<E>, @I java.util.RandomAccess, @I java.lang.Cloneable, @I java.io.Serializable {
  public ArrayList(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public ArrayList() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public ArrayList(@ReadOnly java.util.Collection<? extends E> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void trimToSize() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void ensureCapacity(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int indexOf(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.lang.Object[] toArray() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public <T> T[] toArray(T[] a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E get(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E set(int a1, E a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public E remove(int a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean remove(java.lang.Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@ReadOnly java.util.Collection<? extends E> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean addAll(int a1, @ReadOnly java.util.Collection<? extends E> a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
