package org.checkerframework.afu.scenelib.util.coll;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * A simple implementation of {@link KeyedSet} backed by an insertion-order {@link
 * java.util.LinkedHashMap} and its {@link java.util.LinkedHashMap#values() value collection}.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public class LinkedHashKeyedSet<K, V> extends AbstractSet<V> implements KeyedSet<K, V> {
  /** Produces a key for a value. */
  private final Keyer<? extends K, ? super V> keyer;

  /** The map that backs this set. */
  // Not declared as Map because some implementations of Map prohibit null keys.
  private final LinkedHashMap<K, V> theMap = new LinkedHashMap<>();

  /** The values in the set. Implemented as a view into the map. */
  final Collection<V> theValues = theMap.values();

  /**
   * Constructs a {@link LinkedHashKeyedSet} that uses the given {@link Keyer} to obtain keys for
   * elements.
   */
  public LinkedHashKeyedSet(Keyer<? extends K, ? super V> keyer) {
    this.keyer = keyer;
  }

  @Override
  public int size() {
    return theValues.size();
  }

  @Override
  public boolean contains(Object o) {
    return theValues.contains(o);
  }

  private class KeyedSetIterator implements Iterator<V> {
    private final Iterator<V> itr = theValues.iterator();

    @Override
    public boolean hasNext() {
      return itr.hasNext();
    }

    @Override
    public V next() {
      return itr.next();
    }

    @Override
    public void remove() {
      itr.remove();
    }

    KeyedSetIterator() {}
  }

  @Override
  public Iterator<V> iterator() {
    return new KeyedSetIterator();
  }

  @Override
  public Object[] toArray() {
    return theValues.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return theValues.toArray(a);
  }

  /**
   * Prepares for adding an element to this. Removes the given element if {@code behavior} is
   * REPLACE.
   *
   * @param behavior what action this method should take
   * @param old the element to be removed, if {@code behavior} is REPLACE
   * @return true if an element was removed
   */
  private boolean checkAdd(int behavior, V old) {
    switch (behavior) {
      case REPLACE:
        remove(old);
        return true;
      case IGNORE:
        return false;
      case THROW_EXCEPTION:
        throw new IllegalStateException();
      default:
        throw new IllegalArgumentException();
    }
  }

  private static boolean eq(Object x, Object y) {
    return x == y || (x != null && x.equals(y));
  }

  @Override
  public V add(V o, int conflictBehavior, int equalBehavior) {
    K key = keyer.getKeyFor(o);
    V old = theMap.get(key);
    if (old == null
        || (eq(o, old) ? checkAdd(equalBehavior, old) : checkAdd(conflictBehavior, old)))
      theMap.put(key, o);
    return old;
  }

  @Override
  public boolean add(V o) {
    return add(o, THROW_EXCEPTION, IGNORE) == null;
  }

  @Override
  public boolean remove(Object o) {
    return theValues.remove(o);
  }

  @Override
  public boolean addAll(Collection<? extends V> c) {
    boolean changed = false;
    for (V o : c) {
      changed |= add(o);
    }
    return changed;
  }

  @Override
  public void clear() {
    theValues.clear();
  }

  @Override
  public Keyer<? extends K, ? super V> getKeyer() {
    return keyer;
  }

  @Override
  public V replace(V v) {
    return theMap.put(keyer.getKeyFor(v), v);
  }

  @Override
  public V lookup(K k) {
    return theMap.get(k);
  }

  // Use inherited equals and hashCode algorithms because
  // those of HashMap.Values are broken!
}
