// test case for issue 2493: http://tinyurl.com/cfissue/2493

import org.checkerframework.checker.index.qual.*;

public class Issue2493 {
    public static void test(int a[], int @SameLen("#1") [] b) {
        for (@IndexOrHigh("b") int i = 0; i < a.length; i++) {}
    }
}
