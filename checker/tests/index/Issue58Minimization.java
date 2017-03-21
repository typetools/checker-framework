import org.checkerframework.checker.index.qual.*;

class Issue58Minimization {

    void test(@GTENegativeOne int x) {
        int z;
        if ((z = x) != -1) {
            @NonNegative int y = z;
        }
        if ((z = x) != 1) {
            //:: error: (assignment.type.incompatible)
            @NonNegative int y = z;
        }
    }

    void test2(@NonNegative int x) {
        int z;
        if ((z = x) != 0) {
            @Positive int y = z;
        }
        if ((z = x) == 0) {
            // do nothing
            int y = 5;
        } else {
            @Positive int y = x;
        }
    }
}
