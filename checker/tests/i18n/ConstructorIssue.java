// Test case for issue #2186
// https://github.com/typetools/checker-framework/issues/2186

import java.util.ArrayList;
import org.checkerframework.checker.i18n.qual.*;

@LocalizableKey class ConstructorIssue {
    ConstructorIssue() {}

    @PolyLocalizableKey
    ConstructorIssue(@PolyLocalizableKey String s) {}

    @LocalizableKeyBottom ConstructorIssue(int x) {}

    void test(@LocalizableKeyBottom String str) {
        @LocalizableKey ConstructorIssue obj = new ConstructorIssue();
        @LocalizableKeyBottom ConstructorIssue obj1 = new ConstructorIssue(9);
        @LocalizableKeyBottom ConstructorIssue obj2 = new ConstructorIssue(str);
    }

    void testDiamond() {
        @LocalizableKeyBottom ArrayList<@LocalizableKeyBottom String> list =
                new @LocalizableKeyBottom ArrayList<@LocalizableKeyBottom String>();
    }
}
