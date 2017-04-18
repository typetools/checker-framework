import org.checkerframework.common.value.qual.*;

class MLEqualTo {

    public static void equalToMinLen(int @MinLen(2) [] m, int @MinLen(0) [] r) {
        if (r == m) {
            int @MinLen(2) [] j = r;
        }
    }
}
