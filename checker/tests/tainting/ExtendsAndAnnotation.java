// Test case for issue 278: https://github.com/typetools/checker-framework/issues/278

import org.checkerframework.checker.tainting.qual.*;

class ExtendsAndAnnotation extends @Tainted Object {
    void test(@Untainted ExtendsAndAnnotation c) {
        Object o = new @Untainted ExtendsAndAnnotation();
        o = new @Tainted ExtendsAndAnnotation();
    }
}
