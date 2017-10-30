// Test case for Issue 134:
// https://github.com/typetools/checker-framework/issues/134

import org.checkerframework.checker.nullness.qual.Nullable;

class Wrap<T> {
    class Inner {
        T of(T in) {
            return in;
        }
    }

    Inner get() {
        return new Inner();
    }
}

class Bug {
    void bar(Wrap<Integer> w, Integer f) {
        w.get().of(f).toString();
    }

    void baz(Wrap<@Nullable Integer> w, Integer f) {
        // :: error: (dereference.of.nullable)
        w.get().of(f).toString();
    }
}
