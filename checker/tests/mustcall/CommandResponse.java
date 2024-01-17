// Based on a false positive in Zookeeper.

import java.util.Map;

class CommandResponse {
  Map<String, Object> data;

  public void putAll(Map<? extends String, ?> m) {
    data.putAll(m);
  }

  public void putAll2(Map<? extends String, ? extends Object> m) {
    data.putAll(m);
  }
}
