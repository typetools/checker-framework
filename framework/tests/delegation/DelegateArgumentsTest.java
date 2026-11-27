// Tests that a delegate call must pass the enclosing method's formal parameters, in order.

import java.util.IdentityHashMap;
import org.checkerframework.common.delegation.qual.*;

// :: warning: [delegator.must.override]
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

  @Override
  // :: warning: [invalid.delegate]
  public boolean containsKey(Object key) {
    // The argument is an assignment to the formal parameter, not the caller's argument.
    return map.containsKey(key = "some other key");
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue((value)); // OK
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
