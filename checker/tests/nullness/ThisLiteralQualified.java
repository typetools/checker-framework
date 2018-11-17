// Test case for Issue #2208:
// https://github.com/typetools/checker-framework/issues/2208

// @skip-test until the issue is fixed

import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Raw;

public class ThisLiteralQualified {
    public ThisLiteralQualified() {
        super();
        @UnderInitialization @Raw ThisLiteralQualified a = this;
        @UnderInitialization @Raw ThisLiteralQualified b = ThisLiteralQualified.this;
    }
}
