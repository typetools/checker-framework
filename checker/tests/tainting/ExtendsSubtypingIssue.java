// Test cases for issue 2243
// https://github.com/typetools/checker-framework/issues/2243

import org.checkerframework.checker.tainting.qual.*;

// :: error: (declaration.inconsistent.with.extends.clause)
public @Tainted class ExtendsSubtypingIssue extends Y {}

// :: error: (type.invalid.annotations.on.use)
class ExtendsSubTypingExplicit extends @Untainted X {}

class X {}

@Untainted class Y {
    @Untainted Y() {}
}
