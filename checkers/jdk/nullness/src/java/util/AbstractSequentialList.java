package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class AbstractSequentialList<E extends @NonNull Object> extends java.util.AbstractList<E> {
  protected AbstractSequentialList() {}
  public @Pure E get(int a1) { throw new RuntimeException("skeleton method"); }
  public E set(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public E remove(int a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(int a1, java.util.Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
  public java.util.Iterator<E> iterator() { throw new RuntimeException("skeleton method"); }
  public abstract java.util.ListIterator<E> listIterator(int a1);
}
