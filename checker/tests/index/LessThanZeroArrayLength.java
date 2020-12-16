import org.checkerframework.checker.index.qual.LessThan;

public class LessThanZeroArrayLength {
    void test(int[] a) {
        foo(0, a.length);
    }

    void foo(@LessThan("#2 + 1") int x, int y) {}
}
