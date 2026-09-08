// Tests that a delegate call must pass the enclosing method's formal parameters, in order.

import java.util.IdentityHashMap;
import org.checkerframework.common.delegation.qual.*;

public class DelegateArgumentsTest<K, V> extends IdentityHashMap<K, V> {

  @Delegate private IdentityHashMap<K, V> map;

  @Override
  public V get(Object key) {
    return map.get(key); // OK
  }

  @Override
  // :: warning: [invalid.delegate]
  public V remove(Object key) {
    return map.remove("some other key");
  }
}

class ArgumentOrder {
  public boolean same(Object a, Object b) {
    return a == b;
  }
}

class ArgumentOrderDelegator extends ArgumentOrder {

  @Delegate private ArgumentOrder delegate;

  ArgumentOrderDelegator(ArgumentOrder delegate) {
    this.delegate = delegate;
  }

  @Override
  // :: warning: [invalid.delegate]
  public boolean same(Object a, Object b) {
    return delegate.same(b, a);
  }
}
