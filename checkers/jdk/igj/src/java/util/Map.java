package java.util;
import checkers.igj.quals.*;

@I
public interface Map<K, V> {
  @I
  public interface Entry<K, V> {
    public abstract K getKey(@ReadOnly Entry<K, V> this);
    public abstract V getValue(@ReadOnly Entry<K, V> this);
    public abstract V setValue(@AssignsFields Entry<K, V> this, V a1);
    public abstract boolean equals(@ReadOnly Entry<K, V> this, @ReadOnly Object a1);
    public abstract int hashCode(@ReadOnly Entry<K, V> this);
  }
  public abstract int size(@ReadOnly Map<K, V> this);
  public abstract boolean isEmpty(@ReadOnly Map<K, V> this);
  public abstract boolean containsKey(@ReadOnly Map<K, V> this, @ReadOnly Object a1);
  public abstract boolean containsValue(@ReadOnly Map<K, V> this, @ReadOnly Object a1);
  public abstract V get(@ReadOnly Map<K, V> this, @ReadOnly Object a1) ;
  public abstract V put(@Mutable Map<K, V> this, K a1, V a2);
  public abstract V remove(@Mutable Map<K, V> this, @ReadOnly Object a1);
  public abstract void putAll(@Mutable Map<K, V> this, @ReadOnly Map<? extends K, ? extends V> a1);
  public abstract void clear(@Mutable Map<K, V> this);
  public abstract @I Set<K> keySet(@ReadOnly Map<K, V> this);
  public abstract @I Collection<V> values(@ReadOnly Map<K, V> this);
  public abstract @I Set<@I Map.Entry<K, V>> entrySet(@ReadOnly Map<K, V> this);
  public abstract boolean equals(@ReadOnly Map<K, V> this, @ReadOnly Object a1) ;
  public abstract int hashCode(@ReadOnly Map<K, V> this) ;
}
