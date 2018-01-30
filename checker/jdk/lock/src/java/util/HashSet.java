package java.util;
import org.checkerframework.checker.lock.qual.*;

public class HashSet<E extends Object> extends AbstractSet<E> implements Set<E>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public HashSet() { throw new RuntimeException("skeleton method"); }
  public HashSet(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public HashSet(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public HashSet(int a1) { throw new RuntimeException("skeleton method"); }
  public Iterator<E> iterator() { throw new RuntimeException("skeleton method"); }
   public int size(@GuardSatisfied HashSet<E> this) { throw new RuntimeException("skeleton method"); }
   public boolean isEmpty(@GuardSatisfied HashSet<E> this) { throw new RuntimeException("skeleton method"); }
   public boolean contains(@GuardSatisfied HashSet<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public boolean containsAll(@GuardSatisfied HashSet<E> this, @GuardSatisfied Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(@GuardSatisfied HashSet<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@GuardSatisfied HashSet<E> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@GuardSatisfied HashSet<E> this) { throw new RuntimeException("skeleton method"); }
   public Object clone(@GuardSatisfied HashSet<E> this) { throw new RuntimeException("skeleton method"); }
}
