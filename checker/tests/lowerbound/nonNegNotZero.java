
import org.checkerframework.checker.lowerbound.qual.*;

// @skip-test until the bug is fixed

public class nonNegNotZero {
    public static void main (@NonNegative int i) {
        if (i != 0) {
            @Positive int m = i;
        }
    }
}
