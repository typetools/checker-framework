// Test case for pull request 735
// https://github.com/typetools/checker-framework/pull/735

import org.checkerframework.checker.initialization.qual.UnknownInitialization;

class PptTopLevel {
    class Ppt {
        Object method() {
            return "";
        }
    }

    class OtherPpt extends Ppt {}
}

public class InstanceOf {
    void foo(PptTopLevel.@UnknownInitialization(PptTopLevel.class) Ppt ppt) {
        // :: error: (method.invocation.invalid)
        ppt.method();
        if (ppt instanceof PptTopLevel.OtherPpt) {
            PptTopLevel.OtherPpt pslice = (PptTopLevel.OtherPpt) ppt;
            // :: error: (method.invocation.invalid)
            String samp_str = " s" + pslice.method();
        }
    }
}
