import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MapGetNullable {

    void foo(Map<String, @Nullable Integer> m, @KeyFor("#1") String key) {
        // :: error: (warning.here)
        @NonNull Integer val = m.get(key);
    }
}
