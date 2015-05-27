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
import java.util.Set;

import com.google.common.annotations.GwtCompatible;

/**
 * A map which forwards all its method calls to another map. Subclasses should
 * override one or more methods to modify the behavior of the backing map as
 * desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * @see ForwardingObject
 * @author Kevin Bourrillion
 * @author Jared Levy
 */
@SuppressWarnings("nullness:generic.argument")
@GwtCompatible
    public abstract class ForwardingMap<K extends /*@Nullable*/ Object, V extends /*@Nullable*/ Object> extends ForwardingObject
    implements Map<K, V> {

  @Override protected abstract Map<K, V> delegate();

  @Override
  @Pure public int size() {
    return delegate().size();
  }

  @Override
  @Pure public boolean isEmpty() {
    return delegate().isEmpty();
  }

  @Override
@SuppressWarnings("nullness")
  // Suppressed due to annotations on remove in Java.Map
      public /*@Nullable*/ V remove(/*@Nullable*/ Object object) {
    return delegate().remove(object);
  }

  @Override
public void clear() {
    delegate().clear();
  }

  @Override
@SuppressWarnings("nullness")
  // Suppressed due to annotations on containsKey in Java.Map
  @Pure public boolean containsKey(/*@Nullable*/ Object key) {
    return delegate().containsKey(key);
  }

  @Override
  @SuppressWarnings("nullness")
  // Suppressed due to annotations on containsValue in Java.Map
  @Pure public boolean containsValue(/*@Nullable*/ Object value) {
    return delegate().containsValue(value);
  }

  @Override
  @SuppressWarnings("nullness")
  // Suppressed due to annotations on get in Java.Map
  public V get(/*@Nullable*/ Object key) {
    return delegate().get(key);
  }

  @Override
  public V put(K key, V value) {
    return delegate().put(key, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    delegate().putAll(map);
  }

  @Override
  @SideEffectFree public Set<K> keySet() {
    return delegate().keySet();
  }

  @Override
  @SideEffectFree public Collection<V> values() {
    return delegate().values();
  }

  @Override
  @SideEffectFree public Set<Entry<K, V>> entrySet() {
    return delegate().entrySet();
  }

  @Pure @Override public boolean equals(@Nullable Object object) {
    return object == this || delegate().equals(object);
  }

  @Pure @Override public int hashCode() {
    return delegate().hashCode();
  }
}
