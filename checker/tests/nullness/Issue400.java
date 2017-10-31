import java.util.ArrayList;
import java.util.Collection;

public class Issue400 {
    // :: error: (initialization.fields.uninitialized)
    final class YYPair<T, V> {
        T first;
        V second;
    };

    class YY {
        public Collection<YYPair<String, String>> getX() {
            final Collection<YYPair<String, String>> out = new ArrayList<YYPair<String, String>>();
            out.add(new YYPair<String, String>());
            return out;
        }
    }
}
