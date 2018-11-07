import org.checkerframework.checker.determinism.qual.*;

public class Issue23 {

    public static <T> @PolyDet("up") String arrToString(T[] arr) {
        return arr.toString();
    }

    public static <T> @PolyDet("up") String callToString(T[] arr) {
        return arrToString(arr);
    }

    public static <T> @Det String callToStringDet(T @Det [] arr) {
        return arrToString(arr);
    }

    public static <T> @NonDet String callToStringNonDet(T @NonDet [] arr) {
        return arrToString(arr);
    }

    public static @PolyDet("up") String arrToString1(@PolyDet Object @PolyDet [] arr) {
        return arr.toString();
    }

    public static <T extends @PolyDet Object> @PolyDet("up") String callToString1(
            T @PolyDet [] arr) {
        return arrToString1(arr);
    }

    public static <T extends @Det Object> @Det String callToStringDet1(T @Det [] arr) {
        return arrToString1(arr);
    }

    public static <T extends @NonDet Object> @Det String callToStringDet2(T @Det [] arr) {
        // :: error: (return.type.incompatible)
        return arrToString1(arr);
    }
}
