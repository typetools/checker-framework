import org.checkerframework.checker.minlen.qual.*;

// @skip-test until the bug is fixed

class equalTo {

    public static void equalToMinLen(@MinLen(2) String m, @MinLen(0) String r) {
        if (r == m) {
            @MinLen(2) String j = r;
        }
    }
}
