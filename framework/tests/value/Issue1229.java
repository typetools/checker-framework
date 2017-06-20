import org.checkerframework.common.value.qual.*;

public class Issue1229 {

    Object @ArrayLen(1) [] @ArrayLen(1) [] o3 = new Object[][] {null};

    @IntVal({0, 1, 2, 3}) int[] a1 = new @IntVal({0, 1, 2, 3}) int[] {0, 1, 2, 3};

    int[] a2 = new @IntVal({0, 1, 2, 3}) int[] {0, 1, 2, 3};

    @IntVal({0, 1, 2, 3}) int[] a3 = new int[] {0, 1, 2, 3};

    void test() {

        @IntVal({0, 1, 2, 3}) int[] a1 = new @IntVal({0, 1, 2, 3}) int[] {0, 1, 2, 3};
        int[] a2 = new @IntVal({0, 1, 2, 3}) int[] {0, 1, 2, 3};
        @IntVal({0, 1, 2, 3}) int[] a3 = new int[] {0, 1, 2, 3};
    }
}
