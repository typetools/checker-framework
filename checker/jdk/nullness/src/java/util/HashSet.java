package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public class HashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public HashSet() { throw new RuntimeException("skeleton method"); }
  public HashSet(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public HashSet(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public HashSet(int a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree
  public Iterator<E> iterator() { throw new RuntimeException("skeleton method"); }
  @Pure public int size() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean contains(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Object clone() { throw new RuntimeException("skeleton method"); }
}
