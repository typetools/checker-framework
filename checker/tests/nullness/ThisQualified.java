// Test case for Issue #2208:
// https://github.com/typetools/checker-framework/issues/2208

import org.checkerframework.checker.initialization.qual.UnderInitialization;

public class ThisQualified {
    public ThisQualified() {
        super();
        @UnderInitialization ThisQualified a = this;
        @UnderInitialization ThisQualified b = ThisQualified.this;
    }
}
