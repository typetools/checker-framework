// Test case for issue #2186
// https://github.com/typetools/checker-framework/issues/2186

import org.checkerframework.checker.propkey.qual.*;

@PropertyKey class ConstructorIssue {
    ConstructorIssue() {}
}
