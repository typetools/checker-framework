import java.util.HashMap;
import java.util.Map;
import org.checkerframework.framework.qual.Covariant;

public class Issue6382 {
  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <K, V> Map<K, V> makeMap(Map.Entry<K, V>... entries) {
    Map<K, V> map = new HashMap<>();
    for (Map.Entry<K, V> entry : entries) {
      map.put(entry.getKey(), entry.getValue());
    }
    return map;
  }

  @Covariant({0, 1})
  public static class SimpleEntry<K, V> implements Map.Entry<K, V> {
    private final K key;
    private V value;

    public SimpleEntry(K key, V value) {
      this.key = key;
      this.value = value;
    }

    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(V value) throws UnsupportedOperationException {
      throw new UnsupportedOperationException();
    }
  }

  void method() {

    Issue6382.makeMap(
        new Issue6382.SimpleEntry<>("foo", "quux"), new Issue6382.SimpleEntry<>("baz", null));
  }
}
