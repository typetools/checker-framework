import org.checkerframework.common.value.qual.IntRange;

public class MathMinMax {
    void mathTest(@IntRange(from = 0, to = 20) int x, @IntRange(from = 5, to = 15) int y) {
        @IntRange(from = 0, to = 15) int min = Math.min(x, y);
        @IntRange(from = 5, to = 20) int max = Math.max(x, y);
    }
}
