import org.checkerframework.common.value.qual.*;

public class OneOrTwo {
    @IntVal({1, 2}) int getOneOrTwo() {
        return 1;
    }

    void test(@BottomVal int x) {
        int[] a = new int[Integer.valueOf(getOneOrTwo())];
        // :: error: (array.length.negative)
        int[] b = new int[Integer.valueOf(x)];
    }

    @PolyValue int poly(@PolyValue int y) {
        return y;
    }
}
