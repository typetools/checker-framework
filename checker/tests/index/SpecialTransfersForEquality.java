import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

class SpecialTransfersForEquality {

    void gteN1Test(@GTENegativeOne int y) {
        int[] arr = new int[10];
        if (-1 != y) {
            @NonNegative int z = y;
            if (z < 10) {
                int k = arr[z];
            }
        }
    }

    void nnTest(@NonNegative int i) {
        if (i != 0) {
            @Positive int m = i;
        }
    }
}
