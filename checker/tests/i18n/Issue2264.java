// Test case for issue 2264
// https://github.com/typetools/checker-framework/issues/2264

import org.checkerframework.checker.i18n.qual.LocalizableKey;
import org.checkerframework.checker.i18n.qual.UnknownLocalizableKey;

public class Issue2264 extends SuperClass {
    // :: warning: (inconsistent.constructor.type)
    @LocalizableKey Issue2264() {
        // :: error: (super.invocation.invalid)
        super(9);
    }
}

class ImplicitSuperCall {
    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    @LocalizableKey ImplicitSuperCall() {}
}

class SuperClass {
    @UnknownLocalizableKey SuperClass(int x) {}
}

@LocalizableKey class TestClass {
    // :: error: (type.invalid.annotations.on.use)
    @UnknownLocalizableKey TestClass() {}
}
