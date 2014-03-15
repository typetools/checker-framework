
import java.util.*;

public class InterningTest<V> {
    public boolean lubError(Map.Entry<Object,V> ent) {
        Object v;
        return (v = ent.getValue()) == null;
    }
}
