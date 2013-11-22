// Test case for issue 278: https://code.google.com/p/checker-framework/issues/detail?id=278
// @skip-tests

import checkers.tainting.quals.*;

class ExtendsAndAnnotation extends @Tainted Object {
    void test(@Untainted ExtendsAndAnnotation c) {
    }
}
