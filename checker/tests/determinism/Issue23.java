import org.checkerframework.checker.determinism.qual.*;

public class Issue23 {

    public static <T> String arrToString(T[] arr) {
        return arr.toString();
    }

    public static <T> String callToString(T[] arr) {
        return arrToString(arr);
    }

    public static <T> @Det String callToStringDet(T @Det [] arr) {
        return arrToString(arr);
    }

    public static <T> @NonDet String callToStringNonDet(T @NonDet [] arr) {
        return arrToString(arr);
    }
}
