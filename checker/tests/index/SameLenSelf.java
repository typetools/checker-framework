// Test case for issue 146: https://github.com/kelloggm/checker-framework/issues/146

import org.checkerframework.checker.index.qual.*;

class SameLenSelf {
    int @SameLen("this.field") [] field = new int[10];
    int @SameLen("field2") [] field2 = new int[10];
    int @SameLen("field3") [] field3 = field2;

    void foo(int[] b) {
        int @SameLen("a") [] a = b;
        int @SameLen("c") [] c = new int[10];
    }
}
