// Test case for Issue 1044
// https://github.com/typetools/checker-framework/issues/1044

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue1044 {
    // :: error: (initialization.fields.uninitialized)
    static class Inner1<V> {
        V f;
    }

    // :: error: (initialization.fields.uninitialized)
    static class Inner2<@Nullable T extends @Nullable Object> {
        @NonNull T f;
    }

    static class Inner3<V> {
        V f;
        // :: error: (initialization.fields.uninitialized)
        Inner3() {}
    }

    static class Inner4<@Nullable T extends @Nullable Object> {
        @NonNull T f;
        // :: error: (initialization.fields.uninitialized)
        Inner4() {}
    }

    static class Inner5<V> {
        @Nullable V f;
    }

    static class Inner6<@Nullable T extends @Nullable Object> {
        T f;
    }

    static class Inner7<V> {
        @Nullable V f;

        Inner7() {}
    }

    static class Inner8<@Nullable T extends @Nullable Object> {
        T f;

        Inner8() {}
    }

    // :: error: (initialization.fields.uninitialized)
    static class Inner9<V extends Object> {
        V f;
    }

    static class Inner10<V extends Object> {
        V f;
        // :: error: (initialization.fields.uninitialized)
        Inner10() {}
    }

    static class Inner11<V extends Object> {
        @Nullable V f;
    }

    static class Inner12<V extends Object> {
        @Nullable V f;

        Inner12() {}
    }
}
