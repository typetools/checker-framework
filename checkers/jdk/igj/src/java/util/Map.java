package java.util;
import checkers.igj.quals.*;

@I
public interface Map<K, V> {
  @I
  public interface Entry<K, V> {
    public abstract K getKey(@ReadOnly Entry this);
    public abstract V getValue(@ReadOnly Entry this);
    public abstract V setValue(@AssignsFields Entry this, V a1);
    public abstract boolean equals(@ReadOnly Entry this, @ReadOnly Object a1);
    public abstract int hashCode(@ReadOnly Entry this);
  }
  public abstract int size(@ReadOnly Entry this);
  public abstract boolean isEmpty(@ReadOnly Entry this);
  public abstract boolean containsKey(@ReadOnly Entry this, @ReadOnly Object a1);
  public abstract boolean containsValue(@ReadOnly Entry this, @ReadOnly Object a1);
  public abstract V get(@ReadOnly Entry this, @ReadOnly Object a1) ;
  public abstract V put(@Mutable Entry this, K a1, V a2);
  public abstract V remove(@Mutable Entry this, @ReadOnly Object a1);
  public abstract void putAll(@Mutable Entry this, @ReadOnly Map<? extends K, ? extends V> a1);
  public abstract void clear(@Mutable Entry this);
  public abstract @I Set<K> keySet(@ReadOnly Entry this);
  public abstract @I Collection<V> values(@ReadOnly Entry this);
  public abstract @I Set<@I Map.Entry<K, V>> entrySet(@ReadOnly Entry this);
  public abstract boolean equals(@ReadOnly Entry this, @ReadOnly Object a1) ;
  public abstract int hashCode(@ReadOnly Entry this) ;
}
