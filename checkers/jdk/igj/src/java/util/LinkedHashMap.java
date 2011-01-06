package java.util;
import checkers.igj.quals.*;

@I
public class LinkedHashMap<K, V> extends @I HashMap<K, V> implements @I Map<K, V> {
    private static final long serialVersionUID = 0L;
  public LinkedHashMap(int a1, float a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap(@ReadOnly Map<? extends K, ? extends V> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap(int a1, float a2, boolean a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public V get(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  protected boolean removeEldestEntry(Map.Entry<K, V> entry) { throw new RuntimeException("skeleton method"); }
}
