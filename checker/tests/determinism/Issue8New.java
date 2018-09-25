package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

// @skip-test
class Issue8New {
    public static <T extends @PolyDet("use") String> @PolyDet List<T> copyList(
            @PolyDet List<T> strings) {
        @PolyDet List<T> result = (@PolyDet ArrayList<T>) new @PolyDet ArrayList<T>();
        for (T s : strings) {
            result.add(s);
        }
        return result;
    }

    public static @PolyDet List<@PolyDet("use") String> copyStringList(
            @PolyDet List<@PolyDet("use") String> strings) {
        @PolyDet List<@PolyDet("use") String> result =
                (@PolyDet ArrayList<@PolyDet("use") String>)
                        new @PolyDet ArrayList<@PolyDet("use") String>();
        for (String s : strings) {
            result.add(s.trim());
        }
        return result;
    }
}
