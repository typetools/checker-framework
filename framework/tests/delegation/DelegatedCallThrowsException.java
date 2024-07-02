import java.util.IdentityHashMap;
import java.util.Map;
import org.checkerframework.common.delegation.qual.*;

public class DelegatedCallThrowsException<K, V> extends IdentityHashMap<K, V> {

  private static final long serialVersionUID = -5147442142854693854L;

  /** The wrapped map. */
  @Delegate private final IdentityHashMap<K, V> map;

  private DelegatedCallThrowsException(IdentityHashMap<K, V> map) {
    this.map = map;
  }

  @Override
  public int size(DelegatedCallThrowsException<K, V> this) {
    return map.size();
  }

  @Override
  public boolean isEmpty(DelegatedCallThrowsException<K, V> this) {
    return map.isEmpty();
  }

  @Override
  public V get(DelegatedCallThrowsException<K, V> this, Object key) {
    return map.get(key);
  }

  @Override
  public boolean containsKey(DelegatedCallThrowsException<K, V> this, Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(DelegatedCallThrowsException<K, V> this, Object value) {
    return map.containsValue(value);
  }

  @Override
  public void putAll(DelegatedCallThrowsException<K, V> this, Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException(); // OK
  }
}
