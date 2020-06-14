import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;

// Because the analysis of loops isn't precise enough, the Value Checker issues
// warnings on this test case. So, suppress those warnings, but run the tests
// to make sure that dataflow reaches a fixed point.
@SuppressWarnings("value")
public class OscillatingLoops {

    void oscillatesDoWhile() {
        int i = 0;
        int d = 0;
        do {
            i++;
            if (d > 4566) {
                d = 0;
            } else {
                d++;
            }
        } while (i < Integer.MAX_VALUE);
        @IntRange(from = 0, to = 4567) int after = d;
        @IntVal(Integer.MAX_VALUE) int afterI = i;
    }

    void oscillatesWhile() {
        int i = 0;
        int d = 1;
        while (i < Integer.MAX_VALUE) {
            i++;
            if (d > 4566) {
                d = 0;
            } else {
                d++;
            }
        }
        @IntRange(from = 0, to = 4567) int after = d;
        @IntVal(Integer.MAX_VALUE) int afterI = i;
    }

    void oscillatesDoWhile2() {
        int i = 0;
        int d = 0;
        do {
            if (d > 4566) {
                d = 0;
            } else {
                d++;
            }
            i++;
        } while (i < Integer.MAX_VALUE);
        @IntRange(from = -128, to = 32767) int after = d;
        @IntVal(Integer.MAX_VALUE) int afterI = i;
    }

    void oscillatesFor() {
        int d = 0;
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            if (d > 4566) {
                d = 0;
            } else {
                d++;
            }
        }
        @IntRange(from = -128, to = 32767) int after = d;
    }
}
