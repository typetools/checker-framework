// Test cases for issue 2243
// https://github.com/typetools/checker-framework/issues/2243

import org.checkerframework.checker.tainting.qual.*;

public @Tainted class ExtendsSubtypingIssue extends Y {}

class X {}

@Untainted class Y {
    @Untainted Y() {}
}
