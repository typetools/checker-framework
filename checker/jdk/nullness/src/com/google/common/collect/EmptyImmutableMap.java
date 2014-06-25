/*
 * Copyright (C) 2008 Google Inc.
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

import java.util.Map;

import javax.annotation.Nullable;

import org.checkerframework.dataflow.qual.*;

/**
 * An empty immutable map.
 * 
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 */
@GwtCompatible(serializable = true)
final class EmptyImmutableMap extends ImmutableMap<Object, Object> {
  static final EmptyImmutableMap INSTANCE = new EmptyImmutableMap();

  private EmptyImmutableMap() {}

  @SuppressWarnings("nullness")
  @Override public /*@Nullable*/ Object get(/*@Nullable*/ Object key) {
    return null;
  }

  @Pure public int size() {
    return 0;
  }

  @Pure @Override public boolean isEmpty() {
    return true;
  }

  @Pure @Override public boolean containsKey(/*@Nullable*/ Object key) {
    return false;
  }

  @Pure @Override public boolean containsValue(/*@Nullable*/ Object value) {
    return false;
  }

  @SideEffectFree @Override public ImmutableSet<Entry<Object, Object>> entrySet() {
    return ImmutableSet.of();
  }

  @SideEffectFree @Override public ImmutableSet<Object> keySet() {
    return ImmutableSet.of();
  }

  @SideEffectFree @Override public ImmutableCollection<Object> values() {
    return ImmutableCollection.EMPTY_IMMUTABLE_COLLECTION;
  }

  @Pure @Override public boolean equals(@Nullable Object object) {
    if (object instanceof Map) {
      Map<?, ?> that = (Map<?, ?>) object;
      return that.isEmpty();
    }
    return false;
  }

  @Pure @Override public int hashCode() {
    return 0;
  }

  @Pure @Override public String toString() {
    return "{}";
  }

  Object readResolve() {
    return INSTANCE; // preserve singleton property
  }

  private static final long serialVersionUID = 0;
}
