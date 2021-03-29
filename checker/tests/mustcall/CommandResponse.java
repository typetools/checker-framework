// Based on a false positive in Zookeeper.

import java.util.Map;

class CommandResponse {
  Map<String, Object> data;

  public void putAll(Map<? extends String, ?> m) {
    // This is a false positive. The fix is to change the declaration to match what's below.
    // The cause is that implicit upper bounds are defaulted to top, to match the intuition
    // that e.g. List<E> actually means List<E extends @Top Object>. In this case, that
    // causes an incompatibility with putAll, whose type requires @Bottom Object as the second
    // type parameter, because of the type of the data field.
    // :: error: argument.type.incompatible
    data.putAll(m);
  }

  public void putAll2(Map<? extends String, ? extends Object> m) {
    data.putAll(m);
  }
}
