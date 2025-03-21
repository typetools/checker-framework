package org.checkerframework.javacutil;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.plumelib.util.DeepCopyable;
import org.plumelib.util.UtilPlume;

/** Utility methods related to Java Collections. */
public class CollectionUtils {

  /** Do not instantiate. */
  private CollectionUtils() {
    throw new Error("Do not instantiate");
  }

  //
  // Deprecated utility methods
  //

  /**
   * Creates a LRU cache.
   *
   * @param size size of the cache
   * @return a new cache with the provided size
   * @deprecated use org.plumelib.util.CollectionsPlume.createLruCache
   */
  @Deprecated // 2023-06-02
  public static <K, V> Map<K, V> createLRUCache(int size) {
    return new LinkedHashMap<K, V>(size, .75F, true) {

      private static final long serialVersionUID = 5261489276168775084L;

      @Override
      protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
        return size() > size;
      }
    };
  }

  // A "deep copy" uses the deepCopy() method of the DeepCopyable interface.

  /**
   * Returns a copy of {@code orig}, where each element of the result is a clone of the
   * corresponding element of {@code orig}.
   *
   * @param <T> the type of elements of the collection
   * @param <C> the type of the collection
   * @param orig a collection
   * @return a copy of {@code orig}, as described above
   * @deprecated use org.plumelib.util.CollectionsPlume.cloneElements
   */
  @SuppressWarnings({
    "signedness", // problem with clone()
    "nullness" // generics problem
  })
  @Deprecated // 2023-06-02
  public static <T extends @Nullable Object, C extends @Nullable Collection<T>>
      @PolyNull C cloneElements(@PolyNull C orig) {
    if (orig == null) {
      return null;
    }
    C result = UtilPlume.clone(orig);
    result.clear();
    for (T elt : orig) {
      result.add(UtilPlume.clone(elt));
    }
    return result;
  }

  /**
   * Returns a copy of {@code orig}, where each key and value in the result is a clone of the
   * corresponding element of {@code orig}.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param <M> the type of the map
   * @param orig a map
   * @return a copy of {@code orig}, as described above
   * @deprecated use org.plumelib.util.CollectionsPlume.cloneElements
   */
  @Deprecated // 2023-06-02
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  public static <K, V, M extends @Nullable Map<K, V>> @PolyNull M cloneElements(@PolyNull M orig) {
    return cloneElements(orig, true);
  }

  /**
   * Returns a copy of {@code orig}, where each value of the result is a clone of the corresponding
   * value of {@code orig}, but the keys are the same objects.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param <M> the type of the map
   * @param orig a map
   * @return a copy of {@code orig}, as described above
   * @deprecated use org.plumelib.util.CollectionsPlume.cloneValues
   */
  @Deprecated // 2023-06-02
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  public static <K, V, M extends @Nullable Map<K, V>> @PolyNull M cloneValues(@PolyNull M orig) {
    return cloneElements(orig, false);
  }

  /**
   * Returns a copy of {@code orig}, where each key and value in the result is a clone of the
   * corresponding element of {@code orig}.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param <M> the type of the map
   * @param orig a map
   * @param cloneKeys if true, clone keys; otherwise, re-use them
   * @return a copy of {@code orig}, as described above
   * @deprecated use org.plumelib.util.CollectionsPlume.cloneElements
   */
  @Deprecated // 2023-06-02
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  private static <K, V, M extends @Nullable Map<K, V>> @PolyNull M cloneElements(
      @PolyNull M orig, boolean cloneKeys) {
    if (orig == null) {
      return null;
    }
    M result = UtilPlume.clone(orig);
    result.clear();
    for (Map.Entry<K, V> mapEntry : orig.entrySet()) {
      K oldKey = mapEntry.getKey();
      K newKey = cloneKeys ? UtilPlume.clone(oldKey) : oldKey;
      result.put(newKey, UtilPlume.clone(mapEntry.getValue()));
    }
    return result;
  }

  /**
   * Returns a copy of {@code orig}, where each element of the result is a deep copy (according to
   * the {@code DeepCopyable} interface) of the corresponding element of {@code orig}.
   *
   * @param <T> the type of elements of the collection
   * @param <C> the type of the collection
   * @param orig a collection
   * @return a copy of {@code orig}, as described above
   * @deprecated use org.plumelib.util.CollectionsPlume.deepCopy
   */
  @Deprecated // 2023-06-02
  @SuppressWarnings({"signedness", "nullness:argument"}) // problem with clone()
  public static <T extends @Nullable DeepCopyable<T>, C extends @Nullable Collection<T>>
      @PolyNull C deepCopy(@PolyNull C orig) {
    if (orig == null) {
      return null;
    }
    C result = UtilPlume.clone(orig);
    result.clear();
    for (T elt : orig) {
      result.add(DeepCopyable.deepCopyOrNull(elt));
    }
    return result;
  }

  // The following two methods cannot share an implementation because their generic bounds differ.

  /**
   * Returns a copy of {@code orig}, where each key and value in the result is a deep copy
   * (according to the {@code DeepCopyable} interface) of the corresponding element of {@code orig}.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param <M> the type of the map
   * @param orig a map
   * @return a copy of {@code orig}, as described above
   * @deprecated use org.plumelib.util.CollectionsPlume.deepCopy
   */
  @Deprecated // 2023-06-02
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  public static <
          K extends @Nullable DeepCopyable<K>,
          V extends @Nullable DeepCopyable<V>,
          M extends @Nullable Map<K, V>>
      @PolyNull M deepCopy(@PolyNull M orig) {
    if (orig == null) {
      return null;
    }
    M result = UtilPlume.clone(orig);
    result.clear();
    for (Map.Entry<K, V> mapEntry : orig.entrySet()) {
      K oldKey = mapEntry.getKey();
      V oldValue = mapEntry.getValue();
      result.put(DeepCopyable.deepCopyOrNull(oldKey), DeepCopyable.deepCopyOrNull(oldValue));
    }
    return result;
  }

  /**
   * Returns a copy of {@code orig}, where each value of the result is a deep copy (according to the
   * {@code DeepCopyable} interface) of the corresponding value of {@code orig}, but the keys are
   * the same objects.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param <M> the type of the map
   * @param orig a map
   * @return a copy of {@code orig}, as described above
   * @deprecated use org.plumelib.util.CollectionsPlume.deepCopyValues
   */
  @Deprecated // 2023-06-02
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  public static <K, V extends @Nullable DeepCopyable<V>, M extends @Nullable Map<K, V>>
      @PolyNull M deepCopyValues(@PolyNull M orig) {
    if (orig == null) {
      return null;
    }
    M result = UtilPlume.clone(orig);
    result.clear();
    for (Map.Entry<K, V> mapEntry : orig.entrySet()) {
      K oldKey = mapEntry.getKey();
      V oldValue = mapEntry.getValue();
      result.put(oldKey, DeepCopyable.deepCopyOrNull(oldValue));
    }
    return result;
  }
}
