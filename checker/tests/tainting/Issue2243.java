// Test cases for issue 2243
// https://github.com/typetools/checker-framework/issues/2243

import org.checkerframework.checker.tainting.qual.*;

// :: error: (declaration.inconsistent.with.extends.clause)
public @Tainted class Issue2243 extends Y2243 {}

// :: error: (declaration.inconsistent.with.extends.clause)
class ExtendsSubTypingExplicit2243 extends @Untainted X2243 {}

class X2243 {}

@Untainted class Y2243 {
    // :: warning: (inconsistent.constructor.type) :: error: (super.invocation.invalid)
    @Untainted Y2243() {}
}

@Untainted interface SuperClass2243 {}

// :: error: (declaration.inconsistent.with.implements.clause)
@Tainted class Z2243 implements SuperClass2243 {}

class Issue2243Test {
    @Untainted ExtendsSubTypingExplicit2243 field;
    @Tainted ExtendsSubTypingExplicit2243 field2;
}
