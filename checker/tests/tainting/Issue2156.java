// Test case for issue #2156:
// https://github.com/typetools/checker-framework/issues/2156

// @skip-test until the bug is fixed

import org.checkerframework.checker.tainting.qual.*;

enum SampleEnum {
    @Untainted FIRST,
    @Tainted SECOND;
}

class Issue2156 {
    void test() {
        requireUntainted(SampleEnum.FIRST);
        // :: error: assignment.type.incompatible
        requireUntainted(SampleEnum.SECOND);
    }

    void requireUntainted(@Untainted SampleEnum sEnum) {}
}
