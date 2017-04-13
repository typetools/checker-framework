import org.checkerframework.common.value.qual.*;

class SplitAssignments {
    void foo(@IntRange(from = 5, to = 200) int x) {
        int z;
        if ((z = x) == 5) {
            @IntRange(from = 5, to = 5) int w = x;
            @IntRange(from = 5, to = 5) int q = z;
        }
    }

    void bar(@IntVal({1, 2}) int x) {
        int z;
        if ((z = x) == 1) {
            @IntVal(1) int w = x;
            @IntVal(1) int q = z;
        }
    }
}
