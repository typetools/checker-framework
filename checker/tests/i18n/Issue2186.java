// Test case for issue #2186
// https://github.com/typetools/checker-framework/issues/2186

import java.util.ArrayList;
import org.checkerframework.checker.i18n.qual.*;

@SuppressWarnings("anno.on.irrelevant")
@LocalizableKey class Issue2186 {
    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    Issue2186() {}

    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    @LocalizableKeyBottom Issue2186(int x) {}

    void test() {
        @LocalizableKey Issue2186 obj = new Issue2186();
        @LocalizableKeyBottom Issue2186 obj1 = new Issue2186(9);
    }

    void testDiamond() {
        @LocalizableKeyBottom ArrayList<@LocalizableKeyBottom String> list =
                // :: warning: (cast.unsafe.constructor.invocation)
                new @LocalizableKeyBottom ArrayList<@LocalizableKeyBottom String>();
    }
}
