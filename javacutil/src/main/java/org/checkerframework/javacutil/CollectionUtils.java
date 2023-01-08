package org.checkerframework.javacutil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.plumelib.util.ArrayMap;
import org.plumelib.util.ArraySet;
import org.plumelib.util.CollectionsPlume;

/** Utility methods related to Java Collections. */
public class CollectionUtils {

  /**
   * Creates a LRU cache.
   *
   * @param size size of the cache
   * @return a new cache with the provided size
   */
  public static <K, V> Map<K, V> createLRUCache(final int size) {
    return new LinkedHashMap<K, V>(size, .75F, true) {

      private static final long serialVersionUID = 5261489276168775084L;

      @Override
      protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
        return size() > size;
      }
    };
  }

  // TODO: move into ArrayMap.java.

  /**
   * Returns a new ArrayMap or HashMap with the given capacity. Uses an ArrayMap if the capacity is
   * small, and a HashMap otherwise.
   *
   * @param <K> the type of the keys
   * @param <V> the type of the values
   * @param capacity the expected maximum number of elements in the set
   * @return a new ArrayMap or HashMap with the given capacity
   */
  public static <K, V> Map<K, V> newArrayOrHashMap(int capacity) {
    if (capacity <= 4) {
      return new ArrayMap<>(capacity);
    } else {
      return new HashMap<>(CollectionsPlume.mapCapacity(capacity));
    }
  }

  /**
   * Returns a new ArrayMap or HashMap with the given elements. Uses an ArrayMap if the capacity is
   * small, and a HashMap otherwise.
   *
   * @param <K> the type of the keys
   * @param <V> the type of the values
   * @param m the elements to put in the returned set
   * @return a new ArrayMap or HashMap with the given elements
   */
  public static <K, V> Map<K, V> newArrayOrHashMap(Map<K, V> m) {
    if (m.size() <= 4) {
      return new ArrayMap<>(m);
    } else {
      return new HashMap<>(m);
    }
  }

  /**
   * Returns a new ArrayMap or LinkedHashMap with the given capacity. Uses an ArrayMap if the
   * capacity is small, and a LinkedHashMap otherwise.
   *
   * @param <K> the type of the keys
   * @param <V> the type of the values
   * @param capacity the expected maximum number of elements in the set
   * @return a new ArrayMap or LinkedHashMap with the given capacity
   */
  public static <K, V> Map<K, V> newArrayOrLinkedHashMap(int capacity) {
    if (capacity <= 4) {
      return new ArrayMap<>(capacity);
    } else {
      return new LinkedHashMap<>(CollectionsPlume.mapCapacity(capacity));
    }
  }

  /**
   * Returns a new ArrayMap or LinkedHashMap with the given elements. Uses an ArrayMap if the
   * capacity is small, and a LinkedHashMap otherwise.
   *
   * @param <K> the type of the keys
   * @param <V> the type of the values
   * @param m the elements to put in the returned set
   * @return a new ArrayMap or LinkedHashMap with the given elements
   */
  public static <K, V> Map<K, V> newArrayOrLinkedHashMap(Map<K, V> m) {
    if (m.size() <= 4) {
      return new ArrayMap<>(m);
    } else {
      return new LinkedHashMap<>(m);
    }
  }

  // TODO: move into ArraySet.java.

  /**
   * Returns a new ArraySet or HashSet with the given capacity. Uses an ArraySet if the capacity is
   * small, and a HashSet otherwise.
   *
   * @param <E> the type of the elements
   * @param capacity the expected maximum number of elements in the set
   * @return a new ArraySet or HashSet with the given capacity
   */
  public static <E> Set<E> newArrayOrHashSet(int capacity) {
    if (capacity <= 4) {
      return new ArraySet<>(capacity);
    } else {
      return new HashSet<>(CollectionsPlume.mapCapacity(capacity));
    }
  }

  /**
   * Returns a new ArraySet or HashSet with the given elements. Uses an ArraySet if the capacity is
   * small, and a HashSet otherwise.
   *
   * @param <E> the type of the elements
   * @param s the elements to put in the returned set
   * @return a new ArraySet or HashSet with the given elements
   */
  public static <E> Set<E> newArrayOrHashSet(Set<E> s) {
    if (s.size() <= 4) {
      return new ArraySet<>(s);
    } else {
      return new HashSet<>(s);
    }
  }

  /**
   * Returns a new ArraySet or LinkedHashSet with the given capacity. Uses an ArraySet if the
   * capacity is small, and a LinkedHashSet otherwise.
   *
   * @param <E> the type of the elements
   * @param capacity the expected maximum number of elements in the set
   * @return a new ArraySet or LinkedHashSet with the given capacity
   */
  public static <E> Set<E> newArrayOrLinkedHashSet(int capacity) {
    if (capacity <= 4) {
      return new ArraySet<>(capacity);
    } else {
      return new LinkedHashSet<>(CollectionsPlume.mapCapacity(capacity));
    }
  }

  /**
   * Returns a new ArraySet or LinkedHashSet with the given elements. Uses an ArraySet if the
   * capacity is small, and a LinkedHashSet otherwise.
   *
   * @param <E> the type of the elements
   * @param s the elements to put in the returned set
   * @return a new ArraySet or LinkedHashSet with the given elements
   */
  public static <E> Set<E> newArrayOrLinkedHashSet(Set<E> s) {
    if (s.size() <= 4) {
      return new ArraySet<>(s);
    } else {
      return new LinkedHashSet<>(s);
    }
  }
}
