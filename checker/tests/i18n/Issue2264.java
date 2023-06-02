// Test case for issue 2264
// https://github.com/typetools/checker-framework/issues/2264

import org.checkerframework.checker.i18n.qual.LocalizableKey;
import org.checkerframework.checker.i18n.qual.UnknownLocalizableKey;

@SuppressWarnings("anno.on.irrelevant")
public class Issue2264 extends SuperClass {
  // :: warning: (inconsistent.constructor.type)
  @LocalizableKey Issue2264() {
    // :: error: (super.invocation)
    super(9);
  }
}

@SuppressWarnings("anno.on.irrelevant")
class ImplicitSuperCall {
  // :: error: (super.invocation) :: warning: (inconsistent.constructor.type)
  @LocalizableKey ImplicitSuperCall() {}
}

@SuppressWarnings("anno.on.irrelevant")
class SuperClass {
  @UnknownLocalizableKey SuperClass(int x) {}
}

@SuppressWarnings("anno.on.irrelevant")
@LocalizableKey class TestClass {
  @UnknownLocalizableKey TestClass() {}
}
