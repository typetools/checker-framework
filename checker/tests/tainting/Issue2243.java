// Test cases for issue 2243
// https://github.com/typetools/checker-framework/issues/2243

import org.checkerframework.checker.tainting.qual.*;

// :: error: (declaration.inconsistent.with.extends.clause)
public @Tainted class Issue2243 extends Y {}

// :: error: (super.invocation.invalid)
class ExtendsSubTypingExplicit extends @Untainted X {}

class X {}

@Untainted class Y {
    // :: error: (super.invocation.invalid)
    @Untainted Y() {}
}

@Untainted interface SuperClass {}

// :: error: (declaration.inconsistent.with.implements.clause)
@Tainted class Z implements SuperClass {}
