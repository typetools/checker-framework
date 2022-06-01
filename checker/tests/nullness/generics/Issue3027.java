// Test case for issue #3027:
// https://github.com/typetools/checker-framework/issues/3027

import org.checkerframework.checker.nullness.qual.Nullable;

class Issue3027 {
    class Caller {
        <T, E extends @Nullable T> void foo(Multiset<? extends E> multiset) {
            Entry<? extends E> entry = multiset.someEntry();
        }
    }

    interface Multiset<E> {
        Entry<E> someEntry();
    }

    interface Entry<E> {}
}
