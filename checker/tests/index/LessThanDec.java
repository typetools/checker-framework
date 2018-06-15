import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.IndexOrLow;
import org.checkerframework.checker.index.qual.LessThan;

public class LessThanDec {
    private static @IndexOrLow("#1") @LessThan("#4") int lastIndexOf(
            short[] array, short target, @IndexOrHigh("#1") int start, @IndexOrHigh("#1") int end) {
        for (int i = end - 1; i >= start; i--) {
            if (array[i] == target) {
                return i;
            }
        }
        return -1;
    }
}
