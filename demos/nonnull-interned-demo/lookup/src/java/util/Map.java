package java.util;

import checkers.quals.*;

public abstract interface Map<K, V> {
  public abstract interface Entry<K, V> {
    public abstract K getKey();
    public abstract V getValue();
    public abstract V setValue(V a1);
    public abstract boolean equals(java.lang.Object a1);
    public abstract int hashCode();
  }
  public abstract int size();
  public abstract boolean isEmpty();
  public abstract boolean containsKey(java.lang.Object a1);
  public abstract boolean containsValue(java.lang.Object a1);
  public abstract V get(java.lang.Object a1);
  public abstract V put(K a1, V a2);
  public abstract V remove(java.lang.Object a1);
  public abstract void putAll(java.util.Map<? extends K, ? extends V> a1);
  public abstract void clear();
  public abstract @NonNull java.util.Set<K> keySet();
  public abstract @NonNull java.util.Collection<V> values();
  public abstract @NonNull java.util.Set<@NonNull java.util.Map.Entry<K, V>> entrySet();
  public abstract boolean equals(java.lang.Object a1);
  public abstract int hashCode();
}
