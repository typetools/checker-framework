import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;

public class MathMinMax {
    void mathTest(@IntRange(from = 0, to = 20) int x, @IntRange(from = 5, to = 15) int y) {
        @IntRange(from = 0, to = 15) int min = Math.min(x, y);
        @IntRange(from = 5, to = 20) int max = Math.max(x, y);
        @IntVal(1) int minConst = Math.min(1, 2);
        @IntVal(2) int maxConst = Math.max(-1, 2);
    }

    void mathTest(long x, long y) {
        long min = Math.min(x, y);
        long max = Math.max(x, y);
    }

    void mathTest(double x, double y) {
        double min = Math.min(x, y);
        double max = Math.max(x, y);
    }

    void mathTetMax(@IntRange int x, @IntRange int y) {
        @IntRange int z = Math.min(x, y);
    }
}
