// Test case for issue 278: https://code.google.com/p/checker-framework/issues/detail?id=278

import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

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
