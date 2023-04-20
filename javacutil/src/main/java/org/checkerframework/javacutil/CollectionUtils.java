package org.checkerframework.javacutil;

import java.util.LinkedHashMap;
import java.util.Map;

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

  ///
  /// Temporary utility methods
  ///

  // TODO: After plume-util 1.6.6 is released, use these methods from CollectionsPlume.

  /**
   * Returns a copy of {@code orig}, where each element of the result is a clone of the
   * corresponding element of {@code orig}.
   *
   * @param orig a list
   * @return a deep copy of {@code orig}
   */
  public static <T> List<T> deepCopy(@Nullable List<T> orig) {
    if (orig == null) {
      return null;
    }
    List<T> result = new ArrayList<>(orig.size);
    for (T elt : orig) {
      result.add(elt.clone());
    }
    return result;
  }

  /**
   * Returns a copy of {@code orig}, where each value of the result is a clone of the corresponding
   * value of {@code orig}, but the keys are the same objects.
   *
   * @param orig a map
   * @return a deep copy of {@code orig}
   */
  public static <K, V> Map<K, V> deepCopyValues(Map<K, V> orig) {
    if (orig == null) {
      return null;
    }
    Map<K, V> result = new HashMap<>(orig.size);
    for (Map.Entry<K, V> mapEntry : orig.entrySet()) {
      result.put(mapEntry.getKey(), mapEntry.getValue().clone());
    }
    return result;
  }
}
