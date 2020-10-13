// Test case for Issue #2208:
// https://github.com/typetools/checker-framework/issues/2208

import org.checkerframework.checker.initialization.qual.UnderInitialization;

public class ThisLiteralQualified {
    public ThisLiteralQualified() {
        super();
        @UnderInitialization ThisLiteralQualified a = this;
        @UnderInitialization ThisLiteralQualified b = ThisLiteralQualified.this;
    }
}
