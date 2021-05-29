import java.util.ArrayList;
import java.util.Collection;

public class Issue400 {
  final class YYPair<T, V> {
    // :: error: (initialization.field.uninitialized)
    T first;
    // :: error: (initialization.field.uninitialized)
    V second;
  }

  class YY {
    public Collection<YYPair<String, String>> getX() {
      final Collection<YYPair<String, String>> out = new ArrayList<YYPair<String, String>>();
      out.add(new YYPair<String, String>());
      return out;
    }
  }
}
