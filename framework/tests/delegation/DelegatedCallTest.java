import java.util.IdentityHashMap;
import org.checkerframework.common.delegation.qual.*;

public class DelegatedCallTest<K, V> extends IdentityHashMap<K, V> {

  private static final long serialVersionUID = -5147442142854693854L;

  /** The wrapped map. */
  @Delegate private final IdentityHashMap<K, V> map;

  private DelegatedCallTest(IdentityHashMap<K, V> map) {
    this.map = map;
  }

  @Override
  public int size(DelegatedCallTest<K, V> this) {
    return map.size();
  }

  @Override
  public boolean isEmpty(DelegatedCallTest<K, V> this) {
    return map.isEmpty();
  }

  @Override
  public V get(DelegatedCallTest<K, V> this, Object key) {
    return map.get(key);
  }

  @Override
  public boolean containsKey(DelegatedCallTest<K, V> this, Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(DelegatedCallTest<K, V> this, Object value) {
    return map.containsValue(value);
  }
}
