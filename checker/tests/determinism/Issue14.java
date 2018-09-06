package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

class Issue14 {
    public static <T> Set<T> genericsBug(List<T> list) {
        Set<T> result = new HashSet<T>();
        for (T element : list) {
            result.add(element);
        }
        return result;
    }

    public static <T> @PolyDet Set<T> toSet(@PolyDet List<T> list) {
        @PolyDet Set<T> result = new @PolyDet HashSet<T>();
        for (T element : list) {
            result.add(element);
        }
        return result;
    }

    public static <T extends @PolyDet Object> @PolyDet Set<T> toSet1(
            @PolyDet List<T> list, @PolyDet Set<T> result) {
        for (T element : list) {
            result.add(element);
        }
        return result;
    }

    public static <@PolyDet T extends @PolyDet Object> @PolyDet Set<T> toSet2(
            @PolyDet List<T> list, @PolyDet Set<T> result) {
        for (T element : list) {
            result.add(element);
        }
        return result;
    }
}
