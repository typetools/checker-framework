package determinism;

import java.util.*;
import org.checkerframework.checker.nullness.qual.*;

// @skip-test
class Issue14PolyNUll {
    public static <T extends @Nullable Object> void toSet(List<T> list) {
        @NonNull T a = list.get(0);
    }

    public static <T> @PolyNull Set<T> toSet1(@PolyNull List<T> list) {
        Set<T> result = new HashSet<T>();
        for (@NonNull T element : list) {
            result.add(element);
        }
        return result;
    }
}
