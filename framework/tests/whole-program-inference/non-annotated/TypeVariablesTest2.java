import java.util.HashMap;
import java.util.Map;
import tests.wholeprograminference.qual.*;

public class TypeVariablesTest2<K extends String, V extends Integer> {

    Map<K, V> map = new HashMap<>();

    public V getValue(K key) {
        return map.get(key);
    }
}
