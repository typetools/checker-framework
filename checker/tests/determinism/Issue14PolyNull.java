package determinism;

import java.util.*;
import org.checkerframework.checker.nullness.qual.*;

// @skip-test
class Issue14PolyNUll {
    //    public static <T extends @Nullable Object> void toSet(List<T> list) {
    //        @NonNull T a = list.get(0);
    //    }

    public static <T extends @PolyNull Object> @PolyNull Set<T> toSet1(
            @PolyNull List<T> list, @PolyNull Set<T> result) {
        for (T element : list) {
            result.add(element);
        }
        return result;
    }
}
