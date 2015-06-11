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

import org.checkerframework.checker.nullness.qual.Nullable;
//import javax.annotation.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.annotations.GwtCompatible;

/**
 * A multimap which forwards all its method calls to another multimap.
 * Subclasses should override one or more methods to modify the behavior of
 * the backing multimap as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * @see ForwardingObject
 * @author Robert Konigsberg
 */
@SuppressWarnings("nullness:generic.argument")
@GwtCompatible
    public abstract class ForwardingMultimap<K extends /*@Nullable*/ Object, V extends /*@Nullable*/ Object> extends ForwardingObject
    implements Multimap<K, V> {

  @Override protected abstract Multimap<K, V> delegate();

  @Override
  public Map<K, Collection<V>> asMap() {
    return delegate().asMap();
  }

  @Override
  public void clear() {
    delegate().clear();
  }

  @Override
  @Pure public boolean containsEntry(@Nullable Object key, @Nullable Object value) {
    return delegate().containsEntry(key, value);
  }

  @Override
  @Pure public boolean containsKey(@Nullable Object key) {
    return delegate().containsKey(key);
  }

  @Override
  @Pure public boolean containsValue(@Nullable Object value) {
    return delegate().containsValue(value);
  }

  @Override
  @SideEffectFree public Collection<Entry<K, V>> entries() {
    return delegate().entries();
  }

  @Override
  public Collection<V> get(@Nullable K key) {
    return delegate().get(key);
  }

  @Override
  @Pure public boolean isEmpty() {
    return delegate().isEmpty();
  }

  @Override
  public Multiset<K> keys() {
    return delegate().keys();
  }

  @Override
  @SideEffectFree public Set<K> keySet() {
    return delegate().keySet();
  }

  @Override
  public boolean put(K key, V value) {
    return delegate().put(key, value);
  }

  @Override
  public boolean putAll(K key, Iterable<? extends V> values) {
    return delegate().putAll(key, values);
  }

  @Override
  public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
    return delegate().putAll(multimap);
  }

  @Override
  public boolean remove(@Nullable Object key, @Nullable Object value) {
    return delegate().remove(key, value);
  }

  @Override
  public Collection<V> removeAll(@Nullable Object key) {
    return delegate().removeAll(key);
  }

  @Override
  public Collection<V> replaceValues(K key, Iterable<? extends V> values) {
    return delegate().replaceValues(key, values);
  }

  @Override
  @Pure public int size() {
    return delegate().size();
  }

  @Override
  @SideEffectFree public Collection<V> values() {
    return delegate().values();
  }

  @Pure @Override public boolean equals(@Nullable Object object) {
    return object == this || delegate().equals(object);
  }

  @Pure @Override public int hashCode() {
    return delegate().hashCode();
  }
}
