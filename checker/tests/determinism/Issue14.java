package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

// @skip-test
class Issue14 {
    public static <@PolyDet("use") T extends @PolyDet("use") Object> @PolyDet Set<T> toSet(
            @PolyDet List<T> list, @PolyDet Set<T> result) {
        for (T element : list) {
            result.add(element);
        }
        return result;
    }

    public static <@PolyDet("use") T extends @PolyDet("use") Object> @PolyDet Set<T> toSetNew(
            @PolyDet List<T> list) {
        @PolyDet Set<T> result = new @PolyDet HashSet<T>();
        for (T element : list) {
            result.add(element);
        }
        return result;
    }

    //    public static <@Det T extends @NonDet Object> @NonDet Set<T> toSet1(@NonDet List<T> list)
    // {
    //        @NonDet Set<T> result = new @NonDet HashSet<T>();
    //        for (T element : list) {
    //            result.add(element);
    //        }
    //        return result;
    //    }
    //
    //    public static <@Det T extends @Det Object> @Det Set<T> toSet2(@Det List<T> list) {
    //        @Det Set<T> result = new @Det HashSet<T>();
    //        for (T element : list) {
    //            result.add(element);
    //        }
    //        return result;
    //    }
    //
    //    public static <@Det T extends @Det Object> @OrderNonDet Set<T> toSet3(
    //            @OrderNonDet List<T> list) {
    //        @OrderNonDet Set<T> result = new @OrderNonDet HashSet<T>();
    //        result.add(list.get(0));
    //        return result;
    //    }
    //
    //    public static @NonDet Set<@NonDet List<@Det Integer>> toSet3(
    //            @OrderNonDet List<@OrderNonDet List<@Det Integer>> list) {
    //        @NonDet
    //        Set<@NonDet List<@Det Integer>> result = new @NonDet HashSet<@NonDet List<@Det
    // Integer>>();
    //        result.add(list.get(0));
    //        return result;
    //    }
    //
    //    public static <@PolyDet T extends @PolyDet Object> @PolyDet Set<T> toSet4(
    //            @PolyDet List<T> list) {
    //        @NonDet Set<T> result = new @NonDet HashSet<T>();
    //        for (T element : list) {
    //            result.add(element);
    //        }
    //        return result;
    //    }
}
