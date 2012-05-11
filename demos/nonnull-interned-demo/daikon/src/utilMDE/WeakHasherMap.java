/*
 * @(#)WeakHashMap.java	1.5 98/09/30
 *
 * Copyright 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package utilMDE;

import java.util.Iterator;
import java.util.Map;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Set;
import java.util.AbstractSet;
import java.util.NoSuchElementException;

import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;

import utilMDE.Hasher;

// The purpose of this class is to be used by Intern.java.  It is difficult
// to upgrade this to use the Java 1.5 version of WeakHashMap (which is
// generic, among other advantages) because that implementation uses
// various package-visible implementation details of java.util.
/**
 * This is a modified version of @see{WeakHashMap} from JDK 1.2.2.
 * The modification adds a constructor that takes a Hasher argument.
 * I also replaced uses of hashCode() and equals() by uses of Hasher,
 * where appropriate.
 * I've changed the name so that this version is not used accidentally.
 * It is slightly less efficient, in the absence of a Hasher, and has
 * not been as extensively tested.<p>
 *
 * <hr>
 *
 * A hashtable-based <code>Map</code> implementation with <em>weak keys</em>.
 * An entry in a <code>WeakHashMap</code> will automatically be removed when
 * its key is no longer in ordinary use.  More precisely, the presence of a
 * mapping for a given key will not prevent the key from being discarded by the
 * garbage collector, that is, made finalizable, finalized, and then reclaimed.
 * When a key has been discarded its entry is effectively removed from the map,
 * so this class behaves somewhat differently than other <code>Map</code>
 * implementations.
 *
 * <p> Both null values and the null key are supported.  This class has
 * performance characteristics similar to those of the <code>HashMap</code>
 * class, and has the same efficiency parameters of <em>initial capacity</em>
 * and <em>load factor</em>.
 *
 * <p> Like most collection classes, this class is not synchronized.  A
 * synchronized <code>WeakHashMap</code> may be constructed using the
 * <code>Collections.synchronizedMap</code> method.
 *
 * <p> This class is intended primarily for use with key objects whose
 * <code>equals</code> methods test for object identity using the
 * <code>==</code> operator.  Once such a key is discarded it can never be
 * recreated, so it is impossible to do a lookup of that key in a
 * <code>WeakHashMap</code> at some later time and be surprised that its entry
 * has been removed.  This class will work perfectly well with key objects
 * whose <code>equals</code> methods are not based upon object identity, such
 * as <code>String</code> instances.  With such recreatable key objects,
 * however, the automatic removal of <code>WeakHashMap</code> entries whose
 * keys have been discarded may prove to be confusing.
 *
 * <p> The behavior of the <code>WeakHashMap</code> class depends in part upon
 * the actions of the garbage collector, so several familiar (though not
 * required) <code>Map</code> invariants do not hold for this class.  Because
 * the garbage collector may discard keys at any time, a
 * <code>WeakHashMap</code> may behave as though an unknown thread is silently
 * removing entries.  In particular, even if you synchronize on a
 * <code>WeakHashMap</code> instance and invoke none of its mutator methods, it
 * is possible for the <code>size</code> method to return smaller values over
 * time, for the <code>isEmpty</code> method to return <code>false</code> and
 * then <code>true</code>, for the <code>containsKey</code> method to return
 * <code>true</code> and later <code>false</code> for a given key, for the
 * <code>get</code> method to return a value for a given key but later return
 * <code>null</code>, for the <code>put</code> method to return
 * <code>null</code> and the <code>remove</code> method to return
 * <code>false</code> for a key that previously appeared to be in the map, and
 * for successive examinations of the key set, the value set, and the entry set
 * to yield successively smaller numbers of elements.
 *
 * <p> Each key object in a <code>WeakHashMap</code> is stored indirectly as
 * the referent of a weak reference.  Therefore a key will automatically be
 * removed only after the weak references to it, both inside and outside of the
 * map, have been cleared by the garbage collector.
 *
 * <p> <strong>Implementation note:</strong> The value objects in a
 * <code>WeakHashMap</code> are held by ordinary strong references.  Thus care
 * should be taken to ensure that value objects do not strongly refer to their
 * own keys, either directly or indirectly, since that will prevent the keys
 * from being discarded.  Note that a value object may refer indirectly to its
 * key via the <code>WeakHashMap</code> itself; that is, a value object may
 * strongly refer to some other key object whose associated value object, in
 * turn, strongly refers to the key of the first value object.  This problem
 * may be fixed in a future release.
 *
 * @version	1.5, 98/09/30
 * @author	Mark Reinhold
 * @since	JDK1.2
 * @see		java.util.HashMap
 * @see		java.lang.ref.WeakReference
 */

public final class WeakHasherMap<K,V> extends AbstractMap<K,V> implements Map<K,V> {

    /* A WeakHashMap is implemented as a HashMap that maps WeakKeys to values.
       Because we don't have access to the innards of the HashMap, the various
       query methods must create a temporary WeakKey every time a lookup is
       done.  Fortunately these are small, short-lived objects, so the added
       allocation overhead is tolerable. */

    private Hasher hasher = null;
    private boolean keyEquals(Object k1, Object k2) {
	return (hasher==null ? k1.equals(k2)
			     : hasher.equals(k1, k2));
    }
    private int keyHashCode(Object k1) {
	return (hasher==null ? k1.hashCode()
			     : hasher.hashCode(k1));
    }

    // The WeakKey class can't be static because it depends on the hasher.
    // That in turn means that its methods can't be static.
    // However, I need to be able to call the methods such as create() that
    // were static in the original version of this code.
    // This finesses that.

    private WeakKey WeakKeyCreate(K k) {
	if (k == null) return null;
	else return new WeakKey(k);
    }
    private WeakKey WeakKeyCreate(K k, ReferenceQueue<? super K> q) {
	if (k == null) return null;
	else return new WeakKey(k, q);
    }

    // Cannot be a static class: uses keyHashCode() and keyEquals()
    private final class WeakKey extends WeakReference<K> {
	private int hash;	/* Hashcode of key, stored here since the key
				   may be tossed by the GC */

	private WeakKey(K k) {
	    super(k);
	    hash = keyHashCode(k);
	}

	private WeakKey create(K k) {
	    if (k == null) return null;
	    else return new WeakKey(k);
	}

	private WeakKey(K k, ReferenceQueue<? super K> q) {
	    super(k, q);
	    hash = keyHashCode(k);
	}

	private WeakKey create(K k, ReferenceQueue<? super K> q) {
	    if (k == null) return null;
	    else return new WeakKey(k, q);
	}

        /* A WeakKey is equal to another WeakKey iff they both refer to objects
	   that are, in turn, equal according to their own equals methods */
	public boolean equals(Object o) {
	    if (this == o) return true;
            // This test is illegal because WeakKey is a generic type,
            // so use the getClass hack below instead.
	    // if (!(o instanceof WeakKey)) return false;
            if (!(o.getClass().equals(WeakKey.class))) return false;
	    Object t = this.get();
	    Object u = ((WeakKey)o).get();
	    if ((t == null) || (u == null)) return false;
	    if (t == u) return true;
	    return keyEquals(t, u);
	}

	public int hashCode() {
	    return hash;
	}

    }


    /* Hash table mapping WeakKeys to values */
    private Map<WeakKey,V> hash;

    /* Reference queue for cleared WeakKeys */
    private ReferenceQueue<? super K> queue = new ReferenceQueue<K>();


    /* Remove all invalidated entries from the map, that is, remove all entries
       whose keys have been discarded.  This method should be invoked once by
       each public mutator in this class.  We don't invoke this method in
       public accessors because that can lead to surprising
       ConcurrentModificationExceptions. */
    private void processQueue() {
	WeakKey wk;
	while ((wk = (WeakKey)queue.poll()) != null) { // unchecked cast
	    hash.remove(wk);
	}
    }


    /* -- Constructors -- */

    /**
     * Constructs a new, empty <code>WeakHashMap</code> with the given
     * initial capacity and the given load factor.
     *
     * @param  initialCapacity  The initial capacity of the
     *                          <code>WeakHashMap</code>
     *
     * @param  loadFactor       The load factor of the <code>WeakHashMap</code>
     *
     * @throws IllegalArgumentException  If the initial capacity is less than
     *                                   zero, or if the load factor is
     *                                   nonpositive
     */
    public WeakHasherMap(int initialCapacity, float loadFactor) {
	hash = new HashMap<WeakKey,V>(initialCapacity, loadFactor);
    }

    /**
     * Constructs a new, empty <code>WeakHashMap</code> with the given
     * initial capacity and the default load factor, which is
     * <code>0.75</code>.
     *
     * @param  initialCapacity  The initial capacity of the
     *                          <code>WeakHashMap</code>
     *
     * @throws IllegalArgumentException  If the initial capacity is less than
     *                                   zero
     */
    public WeakHasherMap(int initialCapacity) {
	hash = new HashMap<WeakKey,V>(initialCapacity);
    }

    /**
     * Constructs a new, empty <code>WeakHashMap</code> with the default
     * capacity and the default load factor, which is <code>0.75</code>.
     */
    public WeakHasherMap() {
	hash = new HashMap<WeakKey,V>();
    }

    /**
     * Constructs a new, empty <code>WeakHashMap</code> with the default
     * capacity and the default load factor, which is <code>0.75</code>.
     * The <code>WeakHashMap</code> uses the specified hasher for hashing
     * keys and comparing them for equality.
     */
    public WeakHasherMap(Hasher h) {
	hash = new HashMap<WeakKey,V>();
	hasher = h;
    }


    /* -- Simple queries -- */

    /**
     * Returns the number of key-value mappings in this map.
     * <strong>Note:</strong> <em>In contrast with most implementations of the
     * <code>Map</code> interface, the time required by this operation is
     * linear in the size of the map.</em>
     */
    public int size() {
	return entrySet().size();
    }

    /**
     * Returns <code>true</code> if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
	return entrySet().isEmpty();
    }

    /**
     * Returns <code>true</code> if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     */
    public boolean containsKey(Object key) {
	return hash.containsKey(WeakKeyCreate((K)key)); // unchecked cast
    }


    /* -- Lookup and modification operations -- */

    /**
     * Returns the value to which this map maps the specified <code>key</code>.
     * If this map does not contain a value for this key, then return
     * <code>null</code>.
     *
     * @param  key  The key whose associated value, if any, is to be returned
     */
    public V get(Object key) {  // type of argument is Object, not K
	return hash.get(WeakKeyCreate((K)key)); // unchecked cast
    }

    /**
     * Updates this map so that the given <code>key</code> maps to the given
     * <code>value</code>.  If the map previously contained a mapping for
     * <code>key</code> then that mapping is replaced and the previous value is
     * returned.
     *
     * @param  key    The key that is to be mapped to the given
     *                <code>value</code>
     * @param  value  The value to which the given <code>key</code> is to be
     *                mapped
     *
     * @return  The previous value to which this key was mapped, or
     *          <code>null</code> if if there was no mapping for the key
     */
    public V put(K key, V value) {
	processQueue();
	return hash.put(WeakKeyCreate(key, queue), value);
    }

    /**
     * Removes the mapping for the given <code>key</code> from this map, if
     * present.
     *
     * @param  key  The key whose mapping is to be removed
     *
     * @return  The value to which this key was mapped, or <code>null</code> if
     *          there was no mapping for the key
     */
    public V remove(Object key) { // type of argument is Object, not K
	processQueue();
	return hash.remove(WeakKeyCreate((K)key)); // unchecked cast
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
	processQueue();
	hash.clear();
    }


    /* -- Views -- */


    /* Internal class for entries */
    // This can't be static, again because of dependence on hasher.
    private final class Entry<K,V> implements Map.Entry<K,V> {
	private Map.Entry<WeakKey,V> ent;
	private K key;	/* Strong reference to key, so that the GC
				   will leave it alone as long as this Entry
				   exists */

	Entry(Map.Entry<WeakKey,V> ent, K key) {
	    this.ent = ent;
	    this.key = key;
	}

	public K getKey() {
	    return key;
	}

	public V getValue() {
	    return ent.getValue();
	}

	public V setValue(V value) {
	    return ent.setValue(value);
	}

	private boolean keyvalEquals(K o1, K o2) {
	    return (o1 == null) ? (o2 == null) : keyEquals(o1, o2);
	}

	private boolean valEquals(V o1, V o2) {
	    return (o1 == null) ? (o2 == null) : o1.equals(o2);
	}

	public boolean equals(Map.Entry<K,V> e /* Object o*/) {
            // if (! (o instanceof Map.Entry)) return false;
            // Map.Entry<K,V> e = (Map.Entry<K,V>)o;
	    return (keyvalEquals(key, e.getKey())
		    && valEquals(getValue(), e.getValue()));
	}

	public int hashCode() {
	    V v;
	    return (((key == null) ? 0 : keyHashCode(key))
		    ^ (((v = getValue()) == null) ? 0 : v.hashCode()));
	}

    }


    /* Internal class for entry sets */
    private final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
	Set<Map.Entry<WeakKey,V>> hashEntrySet = hash.entrySet();

	public Iterator<Map.Entry<K,V>> iterator() {

	    return new Iterator<Map.Entry<K,V>>() {
		Iterator<Map.Entry<WeakKey,V>> hashIterator = hashEntrySet.iterator();
		Map.Entry<K,V> next = null;

		public boolean hasNext() {
		    while (hashIterator.hasNext()) {
			Map.Entry<WeakKey,V> ent = hashIterator.next();
			WeakKey wk = ent.getKey();
			K k = null;
			if ((wk != null) && ((k = wk.get()) == null)) {
			    /* Weak key has been cleared by GC */
			    continue;
			}
			next = new Entry<K,V>(ent, k);
			return true;
		    }
		    return false;
		}

		public Map.Entry<K,V> next() {
		    if ((next == null) && !hasNext())
			throw new NoSuchElementException();
		    Map.Entry<K,V> e = next;
		    next = null;
		    return e;
		}

		public void remove() {
		    hashIterator.remove();
		}

	    };
	}

	public boolean isEmpty() {
	    return !(iterator().hasNext());
	}

	public int size() {
	    int j = 0;
	    for (Iterator i = iterator(); i.hasNext(); i.next()) j++;
	    return j;
	}

	public boolean remove(Object o) {
	    processQueue();
	    if (!(o instanceof Map.Entry)) return false;
	    Map.Entry<K,V> e = (Map.Entry<K,V>)o; // unchecked cast
	    Object ev = e.getValue();
	    WeakKey wk = WeakKeyCreate(e.getKey());
	    Object hv = hash.get(wk);
	    if ((hv == null)
		? ((ev == null) && hash.containsKey(wk)) : hv.equals(ev)) {
		hash.remove(wk);
		return true;
	    }
	    return false;
	}

	public int hashCode() {
	    int h = 0;
	    for (Iterator<Map.Entry<WeakKey,V>> i = hashEntrySet.iterator(); i.hasNext(); ) {
		Map.Entry<WeakKey,V> ent = i.next();
		WeakKey wk = ent.getKey();
		Object v;
		if (wk == null) continue;
		h += (wk.hashCode()
		      ^ (((v = ent.getValue()) == null) ? 0 : v.hashCode()));
	    }
	    return h;
	}

    }


    private Set<Map.Entry<K,V>> entrySet = null;

    /**
     * Returns a <code>Set</code> view of the mappings in this map.
     */
    public Set<Map.Entry<K,V>> entrySet() {
	if (entrySet == null) entrySet = new EntrySet();
	return entrySet;
    }

}
