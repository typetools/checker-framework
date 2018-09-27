package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class Issue10 {
    public static String itoa(int value) {
        return String.format("%s", value);
    }

    public void varLenParam(String... str) {
        Issue10 obj = new Issue10();
        @Det int @OrderNonDet [] a = new @Det int @OrderNonDet [10];
        // :: error: (assignment.type.incompatible)
        @Det int x = obj.arrParam(a);
    }

    public @PolyDet("up") int arrParam(int[] str) {
        return str[0];
    }
}
