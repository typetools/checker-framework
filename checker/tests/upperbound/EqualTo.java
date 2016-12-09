import org.checkerframework.checker.upperbound.qual.*;

// @skip-test until the bug is fixed

class equalTo {

    public static void equalToUpper(@LTLengthOf("a") int m, @LTEqLengthOf("a") int r) {
        if (r == m) {
            @LTLengthOf("a") int j = r;
        }
    }
}
