package org.checkerframework.afu.scenelib.util.coll;

import java.util.Iterator;
import java.util.Map;

/**
 * A {@link VivifyingMap} is a map with two additional methods:
 *
 * <ul>
 *   <li>{@link #getVivify} is like {@code get}, but it never returns null; instead, it stores an
 *       empty value and returns that.
 *   <li>{@link #prune} removes empty values
 * </ul>
 */
public abstract class VivifyingMap<K, V> extends WrapperMap<K, V> {
  /**
   * Constructs a new {@link VivifyingMap} backed by the given map. All reads and writes to this
   * {@link VivifyingMap} go through to the backing map. However, since the {@link VivifyingMap}
   * generally provides a superset of the functionality of the backing map, it is rarely useful to
   * access the backing map directly; the parameter is given mainly so you can provide a new map of
   * your favorite class ({@link java.util.HashMap}, {@link java.util.LinkedHashMap}, etc.).
   */
  public VivifyingMap(Map<K, V> back) {
    super(back);
  }

  /**
   * Like {@code get()}, this returns the value to which the specified key is mapped. However, this
   * never returns null. If the key is not currently mapped to a value, this method stores a new
   * empty value, then returns that.
   */
  public V getVivify(K k) {
    if (containsKey(k)) {
      return get(k);
    } else {
      V v = createValueFor(k);
      put(k, v);
      return v;
    }
  }

  /**
   * Returns a new, "empty" value to which the key <code>k</code> can be mapped; subclasses must
   * implement.
   */
  protected abstract V createValueFor(K k);

  /** Prunes this map by deleting entries with empty values. */
  public void prune() {
    // It would be cleaner to write
    //   for (Map.Entry<K, V> entry : entrySet()) {
    // but using an iterator affords efficient deletion.
    for (Iterator<Map.Entry<K, V>> ei = entrySet().iterator(); ei.hasNext(); ) {
      V value = ei.next().getValue();
      if (value instanceof VivifyingMap) {
        ((VivifyingMap) value).prune();
      }
      if (isEmptyValue(value)) {
        ei.remove();
      }
    }
  }

  /**
   * Returns whether the given value is "empty" -- that is, it is the same as what {@link
   * #getVivify} would create.
   *
   * <p>This method does not recursively prune its argument, and it does not need to.
   */
  protected abstract boolean isEmptyValue(V v);
}
