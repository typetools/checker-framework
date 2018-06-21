import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestArraysSort {
    void testSort(@Det int @Det [] a) {
        Arrays.sort(a);
    }

    void testSort1(@Det int @OrderNonDet [] a) {
        // :: error: (argument.type.incompatible)
        System.out.println(a[0]);
        Arrays.sort(a);
        System.out.println(a[0]);
    }
}
