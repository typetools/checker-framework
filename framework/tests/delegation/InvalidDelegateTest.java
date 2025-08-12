import java.util.IdentityHashMap;
import org.checkerframework.common.delegation.qual.*;

// :: warning: (delegate.override)
public class InvalidDelegateTest<K, V> extends IdentityHashMap<K, V> {

  @Delegate public IdentityHashMap<K, V> map;

  @Override
  public boolean isEmpty() {
    return this.map.isEmpty(); // OK
  }

  @Override
  public int size() {
    return map.size(); // OK
  }

  @Override
  // :: warning: (invalid.delegate)
  public boolean containsKey(Object key) {
    // :: error: (contracts.conditional.postcondition)
    return true;
  }

  @Override
  // :: warning: (invalid.delegate)
  public boolean containsValue(Object value) {
    int x = 3;
    return map.containsValue(value);
  }
}
