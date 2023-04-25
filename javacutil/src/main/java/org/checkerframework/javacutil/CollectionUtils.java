package org.checkerframework.javacutil;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.checker.signedness.qual.PolySigned;

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

  // TODO: After code review, copy these methods into plume-util.
  // After plume-util 1.6.6 is released, use these methods from it.

  /**
   * Returns a copy of {@code orig}, where each element of the result is a clone of the
   * corresponding element of {@code orig}.
   *
   * @param <T> the type of elements of the collection
   * @param <C> the type of the collection
   * @param orig a collection
   * @return a deep copy of {@code orig}
   */
  @SuppressWarnings({
    "signedness", // problem with clone()
    "nullness:return" // "return null;"
  })
  public static <@Nullable T, C extends @Nullable Collection<T>> C cloneElements(C orig) {
    if (orig == null) {
      return null;
    }
    C result = clone(orig);
    result.clear();
    for (T elt : orig) {
      result.add(clone(elt));
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
   * @return a deep copy of {@code orig}
   */
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  public static <K, V, M extends Map<K, V>> M cloneElements(M orig) {
    return cloneElements(orig, false);
  }

  /**
   * Returns a copy of {@code orig}, where each value of the result is a clone of the corresponding
   * value of {@code orig}, but the keys are the same objects.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param <M> the type of the map
   * @param orig a map
   * @return a deep copy of {@code orig}
   */
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  public static <K, V, M extends Map<K, V>> M cloneValues(M orig) {
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
   * @return a deep copy of {@code orig}
   */
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  private static <K, V, M extends Map<K, V>> M cloneElements(@PolyNull M orig, boolean cloneKeys) {
    if (orig == null) {
      return null;
    }
    M result = clone(orig);
    result.clear();
    for (Map.Entry<K, V> mapEntry : orig.entrySet()) {
      K oldKey = mapEntry.getKey();
      K newKey = cloneKeys ? clone(oldKey) : oldKey;
      result.put(newKey, clone(mapEntry.getValue()));
    }
    return result;
  }

  /**
   * Returns a copy of {@code orig}, where each element of the result is a deep copy of the
   * corresponding element of {@code orig}.
   *
   * @param <T> the type of elements of the collection
   * @param <C> the type of the collection
   * @param orig a collection
   * @return a deep copy of {@code orig}
   */
  @SuppressWarnings({"signedness", "nullness:argument"}) // problem with clone()
  public static <T extends @Nullable DeepCopyable, C extends @Nullable Collection<T>> @PolyNull C deepCopy(@PolyNull C orig) {
    if (orig == null) {
      return null;
    }
    C result = clone(orig);
    result.clear();
    for (T elt : orig) {
      @SuppressWarnings("unchecked")
      T newElt = elt == null ? elt : (T) elt.deepCopy();
      result.add(newElt);
    }
    return result;
  }

  /**
   * Returns a copy of {@code orig}, where each key and value in the result is a deep copy of the
   * corresponding element of {@code orig}.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param <M> the type of the map
   * @param orig a map
   * @return a deep copy of {@code orig}
   */
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  public static <
          K extends @Nullable DeepCopyable, V extends @Nullable DeepCopyable, M extends Map<K, V>>
      M deepCopy(M orig) {
    if (orig == null) {
      return null;
    }
    M result = clone(orig);
    result.clear();
    for (Map.Entry<K, V> mapEntry : orig.entrySet()) {
      K oldKey = mapEntry.getKey();
      @SuppressWarnings("unchecked")
      K newKey = oldKey == null ? oldKey : (K) oldKey.deepCopy();
      V oldValue = mapEntry.getValue();
      @SuppressWarnings("unchecked")
      V newValue = oldValue == null ? oldValue : (V) oldValue.deepCopy();
      result.put(newKey, newValue);
    }
    return result;
  }

  /**
   * Returns a copy of {@code orig}, where each value of the result is a deep copy of the
   * corresponding value of {@code orig}, but the keys are the same objects.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param <M> the type of the map
   * @param orig a map
   * @return a copy of {@code orig} whose values are deep copies
   */
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  public static <K, V extends DeepCopyable, M extends Map<K, V>> M deepCopyValues(M orig) {
    if (orig == null) {
      return null;
    }
    M result = clone(orig);
    result.clear();
    for (Map.Entry<K, V> mapEntry : orig.entrySet()) {
      K oldKey = mapEntry.getKey();
      K newKey = oldKey;
      V oldValue = mapEntry.getValue();
      @SuppressWarnings("unchecked")
      V newValue = oldValue == null ? oldValue : (V) oldValue.deepCopy();
      result.put(newKey, newValue);
    }
    return result;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Object
  ///

  /**
   * Clones the given object by calling {@code clone()} reflectively. It is not possible to call
   * {@code Object.clone()} directly because it has protected visibility.
   *
   * @param <T> the type of the object to clone
   * @param data the object to clone
   * @return a clone of the object
   */
  @SuppressWarnings({
    "nullness:return", // result of clone() is non-null
    "signedness", // signedness is not relevant
    "unchecked"
  })
  public static <T> @PolyNull @PolySigned T clone(@PolyNull @PolySigned T data) {
    if (data == null) {
      return null;
    }
    try {
      return (T) data.getClass().getMethod("clone").invoke(data);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new Error(e);
    }
  }
}
