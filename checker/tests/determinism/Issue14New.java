package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

// @skip-test
class Issue14New {
    public static <T extends @PolyDet("use") Object> @PolyDet Set<T> toSet(@PolyDet List<T> list) {
        @PolyDet Set<T> result = (@PolyDet HashSet<T>) new HashSet<T>();
        T element = list.get(0);
        result.add(element);
        return result;
    }
}
