import org.checkerframework.checker.index.qual.*;

class Issue58Minimization {

    void test(@GTENegativeOne int x) {
        int z;
        int w = (z = x);
        if ((z = x) != -1) {
            @NonNegative int y = z;
        }
    }
}
