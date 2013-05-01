/*
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import checkers.nullness.quals.*;
//import javax.annotation.Nullable;

/**
 * Factory and utilities pertaining to the {@code MapConstraint} interface.
 *
 * @author Mike Bostock
 */
@SuppressWarnings("nullness:generic.argument")
@GwtCompatible
final class MapConstraints {
  private MapConstraints() {}

  /**
   * Returns a constrained view of the specified entry, using the specified
   * constraint. The {@link Entry#setValue} operation will be verified with the
   * constraint.
   *
   * @param entry the entry to constrain
   * @param constraint the constraint for the entry
   * @return a constrained view of the specified entry
   */
    private static <K extends /*@Nullable*/ Object, V extends /*@Nullable*/ Object> Entry<K, V> constrainedEntry(
      final Entry<K, V> entry,
      final MapConstraint<? super K, ? super V> constraint) {
    checkNotNull(entry);
    checkNotNull(constraint);
    return new ForwardingMapEntry<K, V>() {
      @Override protected Entry<K, V> delegate() {
        return entry;
      }
      @SuppressWarnings("nullness")
      //Suppressed due to typing of checkKeyValue
      @Override public V setValue(V value) {
        constraint.checkKeyValue(getKey(), value);
        return entry.setValue(value);
      }
    };
  }

  /**
   * Returns a constrained view of the specified collection (or set) of entries,
   * using the specified constraint. The {@link Entry#setValue} operation will
   * be verified with the constraint, along with add operations on the returned
   * collection. The {@code add} and {@code addAll} operations simply forward to
   * the underlying collection, which throws an {@link
   * UnsupportedOperationException} per the map and multimap specification.
   *
   * @param entries the entries to constrain
   * @param constraint the constraint for the entries
   * @return a constrained view of the specified entries
   */
  private static <K extends /*@Nullable*/ Object, V extends /*@Nullable*/ Object> Collection<Entry<K, V>> constrainedEntries(
      Collection<Entry<K, V>> entries,
      MapConstraint<? super K, ? super V> constraint) {
    if (entries instanceof Set) {
      return constrainedEntrySet((Set<Entry<K, V>>) entries, constraint);
    }
    return new ConstrainedEntries<K, V>(entries, constraint);
  }

  /**
   * Returns a constrained view of the specified set of entries, using the
   * specified constraint. The {@link Entry#setValue} operation will be verified
   * with the constraint, along with add operations on the returned set. The
   * {@code add} and {@code addAll} operations simply forward to the underlying
   * set, which throws an {@link UnsupportedOperationException} per the map and
   * multimap specification.
   *
   * <p>The returned multimap is not serializable.
   *
   * @param entries the entries to constrain
   * @param constraint the constraint for the entries
   * @return a constrained view of the specified entries
   */
  private static <K extends /*@Nullable*/ Object, V extends /*@Nullable*/ Object> Set<Entry<K, V>> constrainedEntrySet(
      Set<Entry<K, V>> entries,
      MapConstraint<? super K, ? super V> constraint) {
    return new ConstrainedEntrySet<K, V>(entries, constraint);
  }

  static class ConstrainedMap<K extends /*@Nullable*/ Object, V extends /*@Nullable*/ Object> extends ForwardingMap<K, V> {
    final Map<K, V> delegate;
    final MapConstraint<? super K, ? super V> constraint;
    private transient volatile Set<Entry<K, V>> entrySet;

    ConstrainedMap(
        Map<K, V> delegate, MapConstraint<? super K, ? super V> constraint) {
      this.delegate = checkNotNull(delegate);
      this.constraint = checkNotNull(constraint);
    }
    @Override protected Map<K, V> delegate() {
      return delegate;
    }
    @Override public Set<Entry<K, V>> entrySet() {
      if (entrySet == null) {
        entrySet = constrainedEntrySet(delegate.entrySet(), constraint);
      }
      return entrySet;
    }
    
    @SuppressWarnings("nullness") 
    //Suppressed due to typing of checkKeyValue
    @Override public V put(K key, V value) {
      constraint.checkKeyValue(key, value);
      return delegate.put(key, value);
    }
    @Override public void putAll(Map<? extends K, ? extends V> map) {
      delegate.putAll(checkMap(map, constraint));
    }
  }


  /** @see MapConstraints#constrainedEntries */
  private static class ConstrainedEntries<K extends /*@Nullable*/ Object, V extends /*@Nullable*/ Object>
      extends ForwardingCollection<Entry<K, V>> {
    final MapConstraint<? super K, ? super V> constraint;
    final Collection<Entry<K, V>> entries;

    ConstrainedEntries(Collection<Entry<K, V>> entries,
        MapConstraint<? super K, ? super V> constraint) {
      this.entries = entries;
      this.constraint = constraint;
    }
    @Override protected Collection<Entry<K, V>> delegate() {
      return entries;
    }

    @Override public Iterator<Entry<K, V>> iterator() {
      final Iterator<Entry<K, V>> iterator = entries.iterator();
      return new ForwardingIterator<Entry<K, V>>() {
        @Override public Entry<K, V> next() {
          return constrainedEntry(iterator.next(), constraint);
        }
        @Override protected Iterator<Entry<K, V>> delegate() {
          return iterator;
        }
      };
    }

    // See Collections.CheckedMap.CheckedEntrySet for details on attacks.

    @SuppressWarnings("nullness")
    // Suppressed due to annotations on toArray
    @Override public /*@Nullable*/ Object[] toArray() {
      return ObjectArrays.toArrayImpl(this);
    }
    @Override public <T> T[] toArray(T[] array) {
      return ObjectArrays.toArrayImpl(this, array);
    }
    
    @Override public boolean contains(/*@Nullable*/ Object o) {
      return Maps.containsEntryImpl(delegate(), o);
    }
    @Override public boolean containsAll(Collection<?> c) {
      return Collections2.containsAll(this, c);
    }
    
    @Override public boolean remove(/*@Nullable*/ Object o) {
      return Maps.removeEntryImpl(delegate(), o);
    }
    @Override public boolean removeAll(Collection<?> c) {
      return Iterators.removeAll(iterator(), c);
    }
    @Override public boolean retainAll(Collection<?> c) {
      return Iterators.retainAll(iterator(), c);
    }
  }

  static class ConstrainedEntrySet<K extends /*@Nullable*/ Object, V extends /*@Nullable*/ Object>
      extends ConstrainedEntries<K, V> implements Set<Entry<K, V>> {
    ConstrainedEntrySet(Set<Entry<K, V>> entries,
        MapConstraint<? super K, ? super V> constraint) {
      super(entries, constraint);
    }

    // See Collections.CheckedMap.CheckedEntrySet for details on attacks.

    @Override public boolean equals(@Nullable Object object) {
      return Collections2.setEquals(this, object);
    }

    @Override public int hashCode() {
      return Sets.hashCodeImpl(this);
    }
  }

    @SuppressWarnings("nullness")
    // Suppressed to override the annotations on Java.util.Map.Entry
    private static <K extends /*@Nullable*/ Object, V extends /*@Nullable*/ Object> Map<K, V> checkMap(Map<? extends K, ? extends V> map,
      MapConstraint<? super K, ? super V> constraint) {
    Map<K, V> copy = new LinkedHashMap<K, V>(map);
    for (Entry<K, V> entry : copy.entrySet()) {
      constraint.checkKeyValue(entry.getKey(), entry.getValue());
    }
    return copy;
  }
}
