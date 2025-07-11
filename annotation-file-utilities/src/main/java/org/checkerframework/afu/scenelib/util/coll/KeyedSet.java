package org.checkerframework.afu.scenelib.util.coll;

import java.util.Set;

/**
 * A <code>KeyedSet</code> is a set whose members have distinct <em>keys</em> and can be looked up
 * by key. A {@link Keyer} provides keys for the elements. It is forbidden for an element's key to
 * change while the element is in the set.
 */
public interface KeyedSet<K, V> extends Set<V> {
  /**
   * Returns the <code>Keyer</code> that this <code>KeyedSet</code> uses to obtain keys for
   * elements.
   *
   * @return the <code>Keyer</code> that this <code>KeyedSet</code> uses to obtain keys for elements
   */
  public abstract Keyer<? extends K, ? super V> getKeyer();

  // /**
  //  * Calls @{link #add(V, int, int) add(v, THROW_EXCEPTION, IGNORE)} and returns true if <code>v
  //  * </code> was added.
  //  *
  //  * @param v the object to be added
  //  * @return <code>true</code> if <code>v</code> was added
  //  */
  // public abstract boolean add(V v); // causes "ambiguities" for some strange reason

  /** Conflict/equal behavior that does nothing. */
  public static final int IGNORE = -1;

  /** Conflict/equal behavior that throws an {@link IllegalStateException}. */
  public static final int THROW_EXCEPTION = 0;

  /** Conflict/equal behavior that removes the existing object and then adds the new object. */
  public static final int REPLACE = 1;

  /**
   * Adds <code>v</code> to this <code>KeyedSet</code>; this set's <code>Keyer</code> will be called
   * once to fetch <code>v</code>'s key. If an object equal to <code>v</code> is already present in
   * this <code>KeyedSet</code>, then this method carries out the <code>equalBehavior</code> and
   * returns the existing object. Otherwise, if an object having a key equal to <code>v</code>'s is
   * already present in this <code>KeyedSet</code>, then this method carries out the <code>
   * conflictBehavior</code> and returns the existing object. Otherwise, this method adds <code>v
   * </code> to this <code>KeyedSet</code> and returns null.
   *
   * @param v the object to be added
   * @param conflictBehavior specifies what to do if <code>v</code>'s key equals an existing
   *     object's key
   * @param equalBehavior specifies what to do if <code>v</code> equals an existing object
   * @return the existing object whose key matched <code>v</code>'s, if any
   */
  public abstract V add(V v, int conflictBehavior, int equalBehavior);

  /**
   * Adds <code>v</code> to this <code>KeyedSet</code>, replacing and returning an existing object
   * with the same key as <code>v</code> (if any). The existing object is replaced with <code>v
   * </code> even if it equals <code>v</code>. If no existing object is replaced, null is returned.
   */
  public abstract V replace(V v);

  /**
   * Looks for and returns an element with key <code>k</code>, or <code>null</code> if none.
   *
   * @return the element with key <code>k</code>, or <code>null</code> if none
   */
  public abstract V lookup(K k);
}
