import java.util.Arrays;
import org.checkerframework.common.value.qual.MinLen;

public class Issue3224 {

    public static void m1(String @MinLen(1) [] args) {
        int i = 4;
        String @MinLen(1) [] args2 = Arrays.copyOf(args, i);
    }

    public static void m2(String @MinLen(1) ... args) {
        String @MinLen(1) [] args2 = Arrays.copyOf(args, args.length);
    }

    public static void m3(String @MinLen(1) [] args) {
        String @MinLen(1) [] args2 = Arrays.copyOf(args, args.length);
    }
}
