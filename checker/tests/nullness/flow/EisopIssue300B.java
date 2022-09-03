// https://github.com/eisop/checker-framework/issues/300
// The argument could be nullable after an invocation.

import org.checkerframework.checker.nullness.qual.Nullable;

public class EisopIssue300B {
    @Nullable Object f = "";

    void m(Object o) {
        f = null;
    }

    public static void main(String[] args) {
        EisopIssue300B r = new EisopIssue300B();
        if (r.f == null) {
            return;
        }

        r.m(r.f);
        // :: error: (dereference.of.nullable)
        r.f.toString();
    }
}
