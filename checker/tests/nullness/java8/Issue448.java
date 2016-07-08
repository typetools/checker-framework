// Test case for issue 448:
// https://github.com/typetools/checker-framework/issues/448
// @below-java8-jdk-skip-test

import java.util.Arrays;

enum Issue448 {
    ONE;

    void method() {
        Arrays.stream(values()).filter(key -> true);
    }
}
