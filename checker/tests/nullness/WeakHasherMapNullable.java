import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

public abstract class WeakHasherMapNullable<K, V> extends AbstractMap<K, V> implements Map<K, V> {
    private Map<Object, V> hash = new HashMap<>();

    @Pure
    public boolean containsKey(@Nullable Object key) {
        // :: warning: [unchecked] unchecked cast
        K kkey = (K) key;
        // :: error: (argument.type.incompatible)
        hash.containsKey(null);
        // :: error: (contracts.conditional.postcondition.not.satisfied)
        return true;
    }
}
