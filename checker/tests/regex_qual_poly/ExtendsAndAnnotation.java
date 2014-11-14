// Test case for issue 278: https://code.google.com/p/checker-framework/issues/detail?id=278

import org.checkerframework.checker.experimental.regex_qual_poly.qual.*;

@ClassRegexParam("Main")
class A {

}

@ClassRegexParam("Main")
class ExtendsAndAnnotation extends @Wild(param="Main") A {
    void test(@Regex(param="Main") ExtendsAndAnnotation c) {
        Object o = new @Regex(value=1, param="Main") ExtendsAndAnnotation();
        o = new @Regex(param="Main") ExtendsAndAnnotation();
    }
}
