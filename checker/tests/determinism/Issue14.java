package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

// @skip-test
class Issue14 {
    public static <T extends @PolyDet("use") Object> @PolyDet Set<T> toSet(@PolyDet List<T> list) {
        @PolyDet Set<T> result = (@PolyDet HashSet<T>) new HashSet<T>();
        T element = list.get(0);
        // :: error: (argument.type.incompatible)
        result.add(element);
        return result;
    }

    // :: error: (invalid.upper.bound.on.type.argument)
    <T> void testTypeParam(@Det List<T> list) {}

    <T extends @PolyDet("up") Object> void testTypeParam1(@PolyDet List<T> list) {}

    // :: error: (invalid.upper.bound.on.type.argument)
    <T extends @NonDet Object> void toSet(@PolyDet List<T> list, @PolyDet Set<T> result) {
        T element = list.get(0);
        // :: error: (argument.tyep.incompatible)
        result.add(element);
    }
}
