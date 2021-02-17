// Test case for issue 266:
// https://github.com/typetools/checker-framework/issues/266

import org.checkerframework.checker.nullness.qual.*;

public class Issue266 {

    abstract static class Inner {
        abstract String getThing();
    }

    static @Nullable Inner method(@Nullable Object arg) {
        final Object tmp = arg;
        if (tmp == null) {
            return null;
        }
        return new Inner() {
            String getThing() {
                return tmp.toString();
            }
        };
    }
}
