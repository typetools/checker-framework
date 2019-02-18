// Test case for issue #2186
// https://github.com/typetools/checker-framework/issues/2186

import org.checkerframework.checker.propkey.qual.PropertyKey;
import org.checkerframework.checker.propkey.qual.PropertyKeyBottom;

@PropertyKey class ConstructorIssue {
    ConstructorIssue() {}

    @PropertyKeyBottom ConstructorIssue(int x) {}

    void test() {
        //        @PropertyKey ConstructorIssue obj = new ConstructorIssue();
        @PropertyKeyBottom ConstructorIssue obj1 = new ConstructorIssue(9);
        //        @PropertyKeyBottom ConstructorIssue obj2 = new @UnknownPropertyKey
        // ConstructorIssue(9);
    }
}
