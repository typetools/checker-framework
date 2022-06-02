// Test case for issue #3025:
// https://github.com/typetools/checker-framework/issues/3025

import org.checkerframework.checker.nullness.qual.Nullable;

// Classes need to be separate top-level classes to reproduce the issue
class Issue3025Caller {
    <T> void foo(Issue3025Sub<? super T> arg) {
        bar(arg);
        hashCode();
    }

    void bar(Issue3025Sub<?> arg) {}
}

interface Issue3025Super<T> {}

interface Issue3025Sub<T> extends Issue3025Super<@Nullable T> {}
