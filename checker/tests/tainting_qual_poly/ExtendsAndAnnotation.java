// Test case for issue 278: https://github.com/typetools/checker-framework/issues/278

import org.checkerframework.checker.tainting.qual.*;

@ClassTaintingParam("Main")
class A {

}

@ClassTaintingParam("Main")
class ExtendsAndAnnotation extends @Wild(param="Main") A {
    void test(@Untainted(param="Main") ExtendsAndAnnotation c) {
        Object o = new @Untainted(param="Main") ExtendsAndAnnotation();
        o = new @Tainted(param="Main") ExtendsAndAnnotation();
    }
}
