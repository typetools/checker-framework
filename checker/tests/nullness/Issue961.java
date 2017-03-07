import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;

// Test case for Issue 961
// https://github.com/typetools/checker-framework/issues/961
// @skip-test
public class Issue961 {
    <T> T method(T param, Map<T, Object> map) {
        if (map.containsKey(param)) {
            @NonNull Object o = map.get(param);
            return param;
        }
        return null;
    }

    abstract class MapContains<K, V> {
        V def = setDef();
        Map<K, V> map = new HashMap<K, V>();

        V get(K p) {
            if (!map.containsKey(p)) {
                return def;
            }
            return map.get(p);
        }

        abstract V setDef();
    }

    class MapContains2 {
        String get1(Map<Object, String> map, Object k) {
            if (!map.containsKey(k)) {
                return "";
            }
            return map.get(k);
        }

        <KeyTV> String get2(Map<Object, String> map, KeyTV k) {
            if (!map.containsKey(k)) {
                return "";
            }
            return map.get(k);
        }
    }
}
