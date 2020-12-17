import java.util.Map;
import org.checkerframework.checker.nullness.qual.*;

public class Issue328 {
    public static void m(Map<Object, Object> a, Map<Object, Object> b, Object ka, Object kb) {
        if (a.containsKey(ka)) {
            @NonNull Object i = a.get(ka); // OK
        }
        if (b.containsKey(kb)) {
            @NonNull Object i = b.get(kb); // OK
        }
        if (a.containsKey(ka) && b.containsKey(kb)) {
            @NonNull Object i = a.get(ka); // ERROR
            @NonNull Object j = b.get(kb); // ERROR
        }
    }
}
