// Test case for issue 146: https://github.com/kelloggm/checker-framework/issues/146
import org.checkerframework.checker.index.qual.*;

class SameLenSelf {
    void foo(int[] b) {
        int @SameLen("a") [] a = b;
    }
}
