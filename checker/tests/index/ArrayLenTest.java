import org.checkerframework.common.value.qual.*;

public class ArrayLenTest {
    public static String esc_quantify(String /*@ArrayLen({1,2})*/... vars) {
        if (vars.length == 1) {
            return vars[0];
        } else {
            @IntVal({2}) int i = vars.length;
            return vars[0] + vars[1];
        }
    }

    public void test2(int @ArrayLen({1, 2}) [] vars) {
        @IntVal({1, 2}) int w = vars.length;
        if (w == 2) {
            @IntVal({2}) int i = w;
            int j = vars[0] + vars[1];
        } else {
            @IntVal({1}) int i = vars.length;
            int j = vars[0];
        }
    }

    void test(@IntVal({1, 2}) int x) {
        if (x == 1) {
            @IntVal({1}) int y = x;
        }
    }
}
