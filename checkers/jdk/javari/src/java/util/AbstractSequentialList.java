package java.util;
import checkers.javari.quals.*;

public abstract class AbstractSequentialList<E> extends java.util.AbstractList<E> {
  protected AbstractSequentialList() { throw new RuntimeException(("skeleton method")); }
  public E get(int a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E set(int a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public void add(int a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public E remove(int a1) { throw new RuntimeException(("skeleton method")); }
  public boolean addAll(int a1, @ReadOnly java.util.Collection<? extends E> a2) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Iterator<E> iterator() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public abstract @PolyRead java.util.ListIterator<E> listIterator(int a1) @PolyRead;
}
