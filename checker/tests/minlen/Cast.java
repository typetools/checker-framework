import org.checkerframework.checker.minlen.qual.*;

class Cast {
    void test(Object a) {
        int[] b = (int[]) a;
    }
}
