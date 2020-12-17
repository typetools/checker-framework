// Test case for issue #3267:
// https://github.com/typetools/checker-framework/issues/3267

import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue3267 {
    void m1(@Nullable Object obj) {
        if (true) {
            // :: error: (dereference.of.nullable)
            obj.toString();
        }
    }

    void m2(@Nullable Object obj) {
        if (obj != null) {}
        if (true) {
            // :: error: (dereference.of.nullable)
            obj.toString();
        }
    }

    void m3(@Nullable Object obj) {
        if (obj != null) {
        } else {
        }
        if (true) {
            // :: error: (dereference.of.nullable)
            obj.toString();
        }
    }

    void m4(@Nullable Object obj) {
        boolean bool = obj != null;
        if (true) {
            // :: error: (dereference.of.nullable)
            obj.toString();
        }
    }
}
