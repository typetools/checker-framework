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
        T test1(@Nullable Object p) {
            //:: error: (return.type.incompatible)
            return (T) p;
        }

        @SuppressWarnings("unchecked")
        T test2(Object p) {
            return (T) p;
        }
    }

    static class Casts {
        public static void test() {
            Box<String> bs = new Box<String>("");
            bs.f = bs.test1(null);
            //:: error: (assignment.type.incompatiable)
            bs.f = bs.test2(null);
            bs.f.toString();
        }
    }
}
