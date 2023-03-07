
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.nullness.qual.EnsuresKeyFor;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.checker.signedness.qual.PolySigned;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * A map backed by two arrays. It permits null keys and values, and its iterator has deterministic
 * ordering.
 *
 * <p>Compared to a HashMap or LinkedHashMap: For very small maps, this uses much less space, has
 * comparable performance, and (like a LinkedHashMap) is deterministic, with elements returned in
 * the order their keys were inserted. For large maps, this is significantly less performant than
 * other map implementations.
 *
 * <p>Compared to a TreeMap: This uses somewhat less space, and it does not require defining a
 * comparator. This isn't sorted but does have deteriministic ordering. For large maps, this is
 * significantly less performant than other map implementations.
 *
 * <p>A number of other TypeVarPlumeUtil implementations exist, including
 *
 * <ul>
 *   <li>android.util.TypeVarPlumeUtil
 *   <li>com.google.api.client.util.TypeVarPlumeUtil
 *   <li>it.unimi.dsi.fastutil.objects.Object2ObjectTypeVarPlumeUtil
 *   <li>oracle.dss.util.TypeVarPlumeUtil
 *   <li>org.apache.myfaces.trinidad.util.TypeVarPlumeUtil
 * </ul>
 *
 * All of those use the Apache License, version 2.0, whereas this implementation is licensed under
 * the more libral MIT License. In addition, some of those implementations forbid nulls or
 * nondeterministically reorder the contents, and others don't specify their behavior regarding
 * nulls and ordering.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
@SuppressWarnings({
    "index", // TODO
    "keyfor", // https://tinyurl.com/cfissue/4558
    "lock", // not yet annotated for the Lock Checker
    "nullness" // temporary; nullness is tricky because of null-padded arrays
})
public class TypeVarPlumeUtil<K extends Object, V extends Object>
//    extends AbstractMap<K, V>
{

  // An alternate internal representation would be a list of Map.Entry objects (e.g.,
  // AbstractMap.SimpleEntry) instead of two arrays for lists and values.  That is a bad idea
  // because it both uses more memory and makes some operations more expensive.

  /** The keys. Null if capacity=0. */
  private @Nullable K @SameLen("values") [] keys;
  /** The values. Null if capacity=0. */
  private @Nullable V @SameLen("keys") [] values;
  /** The number of used mappings in the representation of this. */
  private @NonNegative @LessThan("keys.length + 1") @IndexOrHigh({"keys", "values"}) int size = 0;
  // An alternate representation would also store the hash code of each key, for quicker querying.

  /**
   * The number of times this map's size has been modified by adding or removing an element
   * (changing the value associated with a key does not count as a change). This field is used to
   * make view iterators fail-fast.
   */
  transient int sizeModificationCount = 0;

  // Constructors

  /**
   * Constructs an empty {@code TypeVarPlumeUtil} with the specified initial capacity.
   *
   * @param initialCapacity the initial capacity
   * @throws IllegalArgumentException if the initial capacity is negative
   */
  @SuppressWarnings({
      "unchecked", // generic array cast
      "samelen:assignment", // initialization
      "allcheckers:purity.not.sideeffectfree.assign.field" // initializes `this`
  })
  public TypeVarPlumeUtil(int initialCapacity) {
    if (initialCapacity < 0)
      throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
    if (initialCapacity == 0) {
      this.keys = null;
      this.values = null;
    } else {
      this.keys = (K[]) new Object[initialCapacity];
      this.values = (V[]) new Object[initialCapacity];
    }
  }

  /**
   * Adds the (key, value) mapping to this.
   *
   * @param index the index of {@code key} in {@code keys}. If -1, add a new mapping. Otherwise,
   *     replace the mapping at {@code index}.
   * @param key the key
   * @param value the value
   */
  @SuppressWarnings({
      "InvalidParam", // Error Prone stupidly warns about field `keys`
      "keyfor:contracts.postcondition" // insertion in keys array suffices
  })
  @EnsuresKeyFor(value = "#2", map = "this")
  private void put(@GTENegativeOne int index, K key, V value) {
    if (index == -1) {
      keys[size] = key;
      values[size] = value;
      size++;
      sizeModificationCount++;
    } else {
      // Replace an existing mapping.
      values[index] = value;
    }
  }

  /**
   * Remove the mapping at the given index. Does nothing if index is -1.
   *
   * @param index the index of the mapping to remove
   * @return true if this map was modified
   */
  private boolean removeIndex(@GTENegativeOne int index) {
    if (index == -1) {
      return false;
    }
    System.arraycopy(keys, index + 1, keys, index, size - index - 1);
    System.arraycopy(values, index + 1, values, index, size - index - 1);
    size--;
    sizeModificationCount++;
    return true;
  }

  /**
   * Returns the index of the given key, or -1 if it does not appear. Uses {@code Objects.equals}
   * for comparison.
   *
   * @param key a key to find
   * @return the index of the given key, or -1 if it does not appear
   */
  private int indexOfKey(@GuardSatisfied @Nullable Object key) {
    if (keys == null) {
      return -1;
    }
    for (int i = 0; i < size; i++) {
      if (Objects.equals(key, keys[i])) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the value at the given index, or null if the index is -1.
   *
   * @param index the index
   * @return the value at the given index, or null if the index is -1
   */
  private @Nullable V getOrNull(@GTENegativeOne int index) {
    return (index == -1) ? null : values[index];
  }

  public @PolyNull V merge(
      K key,
      @NonNull V value,
      BiFunction<? super V, ? super V, ? extends @PolyNull V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    Objects.requireNonNull(value);
    int index = indexOfKey(key);
    V oldValue = getOrNull(index);
    int oldSizeModificationCount = sizeModificationCount;
    @PolyNull V newValue;
    if (oldValue == null) {
      newValue = value;
    } else {
      newValue = remappingFunction.apply(oldValue, value);
    }
    if (oldSizeModificationCount != sizeModificationCount) {
      throw new ConcurrentModificationException();
    }
    if (newValue == null) {
      removeIndex(index);
    } else {
      put(index, key, newValue);
    }
    return newValue;
  }
}
