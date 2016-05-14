// Test case for issue #720:
// https://github.com/typetools/checker-framework/issues/720

// @skip-test until the bug is fixed

import java.util.function.IntConsumer;

class Issue720 {
    static IntConsumer consumer = Test::method;

    static int method(int x) {
        return x;
    }
}
