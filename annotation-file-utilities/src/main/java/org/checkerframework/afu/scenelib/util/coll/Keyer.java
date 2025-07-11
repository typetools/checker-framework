package org.checkerframework.afu.scenelib.util.coll;

/**
 * A {@link Keyer} supplies keys for the elements of a {@link KeyedSet}.
 *
 * @param <K> the key type
 * @param <V> the element type
 */
public interface Keyer<K, V> {
  /** Returns the key that this keyer wishes to assign to the element <code>v</code>. */
  public abstract K getKeyFor(V v);
}
