import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

// Test case for Issue 173
// https://github.com/typetools/checker-framework/issues/173
public abstract class MyMap<K, V> implements Map<K, V> {
  @Override
  @SideEffectsOnly("this")
  // :: error: [contracts.postcondition]
  public @Nullable V put(K key, V value) {
    return null;
  }

  // The call to `put` is permitted because `put` is `@SideEffectsOnly("this")`, and `this` is
  // listed in this method's own annotation.
  @Override
  @SideEffectsOnly("this")
  public void putAll(Map<? extends K, ? extends V> map) {
    for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }
}
