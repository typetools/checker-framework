package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

class TestArrays {
    void testArr(int[] a) {
        // :: error: (assignment.type.incompatible)
        @Det int i = a[0];
    }

    void testArr1(@Det int @Det [] a) {
        @Det int i = a[0];
    }
}
