import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.IntRange;

public class RefineUnknownToIntRange {
    void test1(int x) {
        if (x > 1) {
            @IntRange(from = 2) int z = x;
        }

        if (x < 1) {
            @IntRange(to = 0) int z = x;
        }

        if (1 < x) {
            @IntRange(from = 2) int z = x;
        }

        if (1 > x) {
            @IntRange(to = 0) int z = x;
        }

        if (x >= 1) {
            @IntRange(from = 1) int z = x;
        }

        if (x <= 1) {
            @IntRange(to = 1) int z = x;
        }

        if (x < 100 && x > 2) {
            @IntRange(from = 3, to = 99) int z = x;
        }
    }

    void test3(boolean x) {
        // Make sure non int values are ignored.
        if (x == false) {
            @BoolVal(false) boolean y = x;
        }

        Object o = new Object();
        Object o2 = new Object();
        if (o == o2) {}
    }
}
