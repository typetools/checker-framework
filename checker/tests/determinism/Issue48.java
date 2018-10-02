import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

// @skip-test
public class Issue48 {
    static void testParams(@PolyDet Integer a) {}

    static void callTestParams(@PolyDet List<@PolyDet Integer> list) {
        testParams(list.get(0));
    }

    public static void f(@PolyDet int a) {}

    public static void g(@PolyDet int @PolyDet [] arr) {
        f(arr[0]);
    }
}
