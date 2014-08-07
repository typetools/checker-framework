// Test case for issue 278: https://code.google.com/p/checker-framework/issues/detail?id=278

import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

class ExtendsAndAnnotation extends @Wild Object {
    void test(@Untainted ExtendsAndAnnotation c) {
        Object o = new @Untainted ExtendsAndAnnotation();
        o = new @Tainted ExtendsAndAnnotation();
    }
}
