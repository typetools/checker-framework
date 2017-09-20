import org.checkerframework.common.value.qual.*;

public class Index117 {

    public static void foo(boolean includeIndex, String[] roots) {
        @IntRange(from = 2, to = Integer.MAX_VALUE) int x = (includeIndex ? 2 : 1) * roots.length + 2;
        @IntRange(from = 2, to = Integer.MAX_VALUE) int y = 2 * roots.length + 2;
        @IntRange(from = 2, to = Integer.MAX_VALUE) int z = roots.length + 2;
        @IntRange(from = 2, to = 2) int w = 0 + 2;
    }
}
