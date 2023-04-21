package org.checkerframework.javacutil;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.checker.signedness.qual.PolySigned;
import org.plumelib.util.ArraySet;

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

  // TODO: After plume-util 1.6.6 is released, use this methods from it

  /**
   * Returns a copy of {@code orig}, where each element of the result is a clone of the
   * corresponding element of {@code orig}.
   *
   * @param <T> the type of elements of the list
   * @param orig a list
   * @return a deep copy of {@code orig}
   */
  @SuppressWarnings("signedness") // problem with clone()
  public static <@Nullable T> @PolyNull List<T> deepCopy(@PolyNull List<T> orig) {
    if (orig == null) {
      return null;
    }
    List<T> result = new ArrayList<>(orig.size());
    for (T elt : orig) {
      result.add(clone(elt));
    }
    return result;
  }

  /**
   * Returns a copy of {@code orig}, where each element of the result is a clone of the
   * corresponding element of {@code orig}.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param orig a map
   * @return a deep copy of {@code orig}
   */
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  public static <K, V> @PolyNull Map<K, V> deepCopy(@PolyNull Map<K, V> orig) {
    if (orig == null) {
      return null;
    }
    Map<K, V> result = new HashMap<>(orig.size());
    for (Map.Entry<K, V> mapEntry : orig.entrySet()) {
      result.put(clone(mapEntry.getKey()), clone(mapEntry.getValue()));
    }
    return result;
  }

  /**
   * Returns a copy of {@code orig}, where each value of the result is a clone of the corresponding
   * value of {@code orig}, but the keys are the same objects.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param orig a map
   * @return a deep copy of {@code orig}
   */
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  public static <K, V> @PolyNull Map<K, V> deepCopyValues(@PolyNull Map<K, V> orig) {
    if (orig == null) {
      return null;
    }
    Map<K, V> result = new HashMap<>(orig.size());
    for (Map.Entry<K, V> mapEntry : orig.entrySet()) {
      result.put(mapEntry.getKey(), clone(mapEntry.getValue()));
    }
    return result;
  }

  /**
   * Returns a copy of {@code orig}, where each element of the result is a clone of the
   * corresponding element of {@code orig}.
   *
   * @param <T> the type of elements of the list
   * @param orig a list
   * @return a deep copy of {@code orig}
   */
  @SuppressWarnings("signedness") // problem with UtilPlume.clone()
  public static <@Nullable T> @PolyNull TreeSet<T> deepCopy(@PolyNull TreeSet<T> orig) {
    if (orig == null) {
      return null;
    }
    TreeSet<T> result = new TreeSet<>(orig.comparator());
    for (T elt : orig) {
      result.add(clone(elt));
    }
    return result;
  }

  // This should be an instance method of ArraySet.
  /**
   * Returns a copy of {@code orig}, where each element of the result is a clone of the
   * corresponding element of {@code orig}.
   *
   * @param <T> the type of elements of the list
   * @param orig a list
   * @return a deep copy of {@code orig}
   */
  @SuppressWarnings({
    "nullness:type.argument",
    "mustcall:type.argument"
  }) // problem with UtilPlume.clone()
  public static <@Nullable T> @PolyNull ArraySet<T> deepCopy(@PolyNull ArraySet<T> orig) {
    if (orig == null) {
      return null;
    }
    ArraySet<T> result = new ArraySet<>(orig.size());
    for (T elt : orig) {
      result.add(clone(elt));
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
