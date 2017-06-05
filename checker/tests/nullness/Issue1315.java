// Test case for Issue 1315
// https://github.com/typetools/checker-framework/issues/1315

import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue1315 {
    static class Box<T> {
        T f;

        Box(T p) {
            f = p;
        }

        @SuppressWarnings("unchecked")
        T unsound(@Nullable Object p) {
            //:: error: (return.type.incompatible)
            return (T) p;
        }
    }

    static class Casts {
        public static void test() {
            Box<String> bs = new Box<String>("");
            bs.f = bs.unsound(null);
            bs.f.toString();
        }
    }
}
