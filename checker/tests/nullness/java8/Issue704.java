// Test case for Issue 704:
// https://github.com/typetools/checker-framework/issues/704
// @below-java8-jdk-skip-test

import java.util.function.IntSupplier;

interface Issue704 {
    IntSupplier zero = () -> 0;
}
