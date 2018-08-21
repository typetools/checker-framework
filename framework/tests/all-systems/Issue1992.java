// Test case for Issue 1992:
// https://github.com/typetools/checker-framework/issues/1992

import java.util.List;
import java.util.function.Function;

@SuppressWarnings("") // Check for crashes only
class Issue1992 {

    interface A {}

    static class B<T extends A> {
        C a;
        T b;
    }

    static class C {
        Function<? super A, E> c;

        enum E {
            NONE
        }
    }

    boolean f(List<B<?>> x) {
        B<?> d = x.get(x.size() - 1);
        return d.a.c.apply(d.b) != C.E.NONE;
    }
}
