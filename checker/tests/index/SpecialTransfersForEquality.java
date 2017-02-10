import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

@SuppressWarnings("upperbound")
class SpecialTransfersForEquality {

    void gteN1Test(@GTENegativeOne int y) {
        int[] arr = new int[10];
        if (-1 != y) {
            int k = arr[y];
        }
    }

    void nnTest(@NonNegative int i) {
        if (i != 0) {
            @Positive int m = i;
        }
    }
}
