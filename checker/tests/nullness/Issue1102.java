// Test case for Issue 1102:
// https://github.com/typetools/checker-framework/issues/1102
// Additional test in framework/tests/all-systems/Issue1102.java

import org.checkerframework.checker.nullness.qual.Nullable;

interface Issue1102Itf {}

class Issue1102Base {}

class Issue1102Decl extends Issue1102Base {
    static <S extends Object, T extends Issue1102Base & Issue1102Itf> Issue1102Decl newInstance(
            T s) {
        return new Issue1102Decl();
    }
}

class Issue1102Use<U extends Issue1102Base & Issue1102Itf> {
    @SuppressWarnings("initialization.field.uninitialized")
    U f;

    @Nullable U g = null;

    void bar() {
        Issue1102Decl d = Issue1102Decl.newInstance(f);
        // :: error: (type.argument.type.incompatible)
        d = Issue1102Decl.newInstance(g);
    }
}
