package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

// @skip-test
class Issue8 {
    public static @PolyDet List<@PolyDet String> copyList2(@PolyDet List<@PolyDet String> strings) {
        @PolyDet List<@PolyDet String> copy;
        copy = new @PolyDet ArrayList<@PolyDet String>();
        for (String s : strings) {
            copy.add(s);
        }
        return copy;
    }

    public static @PolyDet List<@PolyDet String> copyList1(
            @PolyDet List<@PolyDet String> strings, @PolyDet List<@PolyDet String> copy) {
        for (String s : strings) {
            copy.add(s);
        }
        return copy;
    }

    public static <T> @PolyDet List<T> copyList(@PolyDet List<T> strings) {
        @PolyDet List<T> copy;
        copy = new @PolyDet ArrayList<T>();
        for (T s : strings) {
            copy.add(s);
        }
        return copy;
    }

    void callCopy(@NonDet List<@NonDet String> str) {
        @NonDet List<@NonDet String> result = copyList(str);
    }

    public static <T extends @NonDet String> List<T> copyList(List<T> strings) {
        List<T> result = new ArrayList<>();
        for (T s : strings) {
            result.add(s.trim());
        }
        return result;
    }
}
