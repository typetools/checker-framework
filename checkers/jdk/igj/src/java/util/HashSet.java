package java.util;
import checkers.igj.quals.*;

@I
public class HashSet<E> extends @I java.util.AbstractSet<E> implements @I java.util.Set<E>, @I java.lang.Cloneable, @I java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public HashSet() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public HashSet(@ReadOnly java.util.Collection<? extends E> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public HashSet(int a1, float a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public HashSet(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public @I java.util.Iterator<E> iterator() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean remove(@ReadOnly java.lang.Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
