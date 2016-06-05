import tests.wholeprograminference.qual.*;
import java.util.Map;
import java.util.HashMap;
public class TypeVariablesTest2<K extends String, V extends Integer> {

    Map<K, V> map = new HashMap<>();

    public V getValue(K key) {
        return map.get(key);
    }
}
