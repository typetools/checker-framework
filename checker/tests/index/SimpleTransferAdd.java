import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

class SimpleTransferAdd {
    void test() {
        int bs = -1;
        // :: error: (assignment.type.incompatible)
        @NonNegative int es = bs;

        // @NonNegative int ds = 2 + bs;
        int ds = 0;
        // :: error: (assignment.type.incompatible)
        @Positive int cs = ds++;
        @Positive int fs = ds;
    }
}
// a comment
