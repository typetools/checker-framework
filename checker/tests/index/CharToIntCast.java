// Test case for issue #2540: https://github.com/typetools/checker-framework/issues/2540

import org.checkerframework.common.value.qual.IntRange;

public class CharToIntCast {

    public static void charRange(char c) {
        @IntRange(from = 0, to = Character.MAX_VALUE) int i = c;
    }

    public static void charShift(char c) {
        char c2 = (char) (c >> 4);
    }

    public static void rangeShiftOk(@IntRange(from = 0, to = Character.MAX_VALUE) int i) {
        char c2 = (char) (i >> 4);
    }
}
