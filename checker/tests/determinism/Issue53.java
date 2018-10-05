import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

// @skip-test
public class Issue53<T extends @NonDet Object> {

    public static <U extends @PolyDet("use") Object> U @PolyDet [] f(@PolyDet Issue53<U> h) {
        return null;
    }

    public static <U extends @PolyDet("use") Object> U @PolyDet [] g(U @PolyDet [] a) {
        return f((@PolyDet Issue53<U>) new @PolyDet Issue53<U>());
    }

    public static <U extends @PolyDet("use") Object> U @PolyDet [] g1(
            U @PolyDet [] a, @PolyDet Issue53<U> inp) {
        return f(inp);
    }
}
