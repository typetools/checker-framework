import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.regex.qual.*;

public abstract class WeakHasherMapNonNull<K, V> extends AbstractMap<K, V> implements Map<K, V> {
  private Map<Object, V> hash = new HashMap<>();

  @org.checkerframework.dataflow.qual.Pure
  public boolean containsKey(@NonNull Object key) {
    // :: warning: [unchecked] unchecked cast
    K kkey = (K) key;
    // :: error: (argument.type.incompatible)
    hash.containsKey(null);
    // :: error: (contracts.conditional.postcondition.not.satisfied)
    return true;
  }
}
