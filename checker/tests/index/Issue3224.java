// Test case for https://tinyurl.com/cfissue/3224

import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.MinLen;

import java.util.Arrays;

public class Issue3224 {
    static class Arrays {
        static String[] copyOf(String[] args, int length) {
            return args;
        }
    }

    public static void m1(String @MinLen(1) [] args) {
        int i = 4;
        String @MinLen(1) [] args2 = java.util.Arrays.copyOf(args, i);
    }

    public static void m2(String @MinLen(1) [] args) {
        String @MinLen(1) [] args2 = java.util.Arrays.copyOf(args, args.length);
    }

    public static void m3(String @MinLen(1) ... args) {
        String @MinLen(1) [] args2 = java.util.Arrays.copyOf(args, args.length);
    }

    public static void m4(String @MinLen(1) [] args, @IntRange(from = 10, to = 200) int len) {
        String @MinLen(1) [] args2 = java.util.Arrays.copyOf(args, len);
    }

    public static void m5(String @MinLen(1) [] args, String[] otherArray) {
        // :: error: assignment.type.incompatible
        String @MinLen(1) [] args2 = java.util.Arrays.copyOf(args, otherArray.length);
    }

    public static void m6(String @MinLen(1) [] args) {
        // :: error: assignment.type.incompatible
        String @MinLen(1) [] args2 = Arrays.copyOf(args, args.length);
    }
}
