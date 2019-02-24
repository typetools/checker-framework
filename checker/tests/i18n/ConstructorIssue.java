// Test case for issue #2186
// https://github.com/typetools/checker-framework/issues/2186

import java.util.ArrayList;
import org.checkerframework.checker.i18n.qual.*;

@LocalizableKey class ConstructorIssue {
    ConstructorIssue() {}

    @LocalizableKeyBottom ConstructorIssue(int x) {}

    void test() {
        @LocalizableKey ConstructorIssue obj = new ConstructorIssue();
        @LocalizableKeyBottom ConstructorIssue obj1 = new ConstructorIssue(9);
    }

    void testDiamond() {
        @LocalizableKeyBottom ArrayList<@LocalizableKeyBottom String> list =
                new @LocalizableKeyBottom ArrayList<@LocalizableKeyBottom String>();
    }
}
