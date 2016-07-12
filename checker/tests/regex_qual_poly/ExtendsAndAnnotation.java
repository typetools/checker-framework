// Test case for issue 278: https://github.com/typetools/checker-framework/issues/278

import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

@ClassRegexParam("Main")
class AExtendsAndAnnotation {}

@ClassRegexParam("Main")
class ExtendsAndAnnotation extends @Wild(param = "Main") AExtendsAndAnnotation {
    void test(@Regex(param = "Main") ExtendsAndAnnotation c) {
        Object o = new @Regex(value = 1, param = "Main") ExtendsAndAnnotation();
        o = new @Regex(param = "Main") ExtendsAndAnnotation();
    }
}
