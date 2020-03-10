import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

class MapMerge {
    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("k", "v");
        // :: error: (return.type.incompatible)
        map.merge("k", "v", (a, b) -> null).toString();
    }

    void foo(Map<String, String> map) {
        // :: error: (return.type.incompatible)
        merge(map, "k", "v", (a, b) -> null).toString();
    }

    <K, V> V merge(
            Map<K, V> map,
            K key,
            V value,
            BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return value;
    }
}
