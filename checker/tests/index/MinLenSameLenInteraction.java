import org.checkerframework.checker.index.qual.*;

class MinLenSameLenInteraction {
    void test(int @SameLen("#2") [] a, int @SameLen("#1") [] b) {
        if (a.length == 1) {
            int x = b[0];
        }
    }
}
