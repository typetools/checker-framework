import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

// Test case for Issue 173
// https://github.com/typetools/checker-framework/issues/173
public abstract class MyMap<K, V> implements Map<K, V> {
    @Override
    // :: error: (contracts.postcondition.not.satisfied)
    public @Nullable V put(K key, V value) {
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
}
