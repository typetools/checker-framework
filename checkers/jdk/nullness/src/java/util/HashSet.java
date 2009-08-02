package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class HashSet<E extends @Nullable Object> extends java.util.AbstractSet<E> implements java.util.Set<E>, java.lang.Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public HashSet() { throw new RuntimeException("skeleton method"); }
  public HashSet(java.util.Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public HashSet(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public HashSet(int a1) { throw new RuntimeException("skeleton method"); }
  public java.util.Iterator<E> iterator() { throw new RuntimeException("skeleton method"); }
  public int size() { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public boolean contains(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
}
