// Test case for issue #720:
// https://github.com/typetools/checker-framework/issues/720
// @below-java8-jdk-skip-test

import java.util.function.IntConsumer;

class Issue720 {
    static IntConsumer consumer = Issue720::method;

    static int method(int x) {
        return x;
    }
}
