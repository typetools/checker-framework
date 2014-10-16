// Test case for issue 278: https://code.google.com/p/checker-framework/issues/detail?id=278

import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

@TaintingParam("Main")
class A {

}

@TaintingParam("Main")
class ExtendsAndAnnotation extends @Wild(target="Main") A {
    void test(@Untainted(target="Main") ExtendsAndAnnotation c) {
        Object o = new @Untainted(target="Main") ExtendsAndAnnotation();
        o = new @Tainted(target="Main") ExtendsAndAnnotation();
    }
}
