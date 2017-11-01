// Test case for pull request 735
// https://github.com/typetools/checker-framework/pull/735

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Raw;

class PptTopLevel {
    class Ppt {
        Object method() {
            return "";
        }
    }

    class OtherPpt extends Ppt {}
}

class InstanceOf {
    void foo(PptTopLevel.@UnknownInitialization(PptTopLevel.class) @Raw Ppt ppt) {
        // :: error: (method.invocation.invalid)
        ppt.method();
        if (ppt instanceof PptTopLevel.OtherPpt) {
            PptTopLevel.OtherPpt pslice = (PptTopLevel.OtherPpt) ppt;
            // :: error: (method.invocation.invalid)
            String samp_str = " s" + pslice.method();
        }
    }
}
