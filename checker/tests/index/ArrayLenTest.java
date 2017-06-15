import org.checkerframework.common.value.qual.*;

public class ArrayLenTest {
    public static String esc_quantify(String @ArrayLen({1, 2}) ... vars) {
        if (vars.length == 1) {
            return vars[0];
        } else {
            @IntVal({2}) int i = vars.length;
            String @ArrayLen({2}) [] a = vars;
            return vars[0] + vars[1];
        }
    }
}
