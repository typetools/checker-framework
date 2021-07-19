import org.checkerframework.common.value.qual.MinLen;

public class Issue2505 {
    public static void warningIfStatement(int @MinLen(1) [] a) {
        int i = a.length;
        if (--i >= 0) {
            a[i] = 0;
        }
    }
}
