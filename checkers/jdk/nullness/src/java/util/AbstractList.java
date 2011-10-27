package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class AbstractList<E extends @Nullable Object> extends AbstractCollection<E> implements List<E> {
  protected AbstractList() {}
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public abstract @Pure E get(int a1);
  public E set(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public E remove(int a1) { throw new RuntimeException("skeleton method"); }
  public int indexOf(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public boolean addAll(int a1, Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
  public Iterator<E> iterator() { throw new RuntimeException("skeleton method"); }
  public ListIterator<E> listIterator() { throw new RuntimeException("skeleton method"); }
  public ListIterator<E> listIterator(int a1) { throw new RuntimeException("skeleton method"); }
  public List<E> subList(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
}
