// Test case for Issue 1633:
// https://github.com/typetools/checker-framework/issues/1633

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.Covariant;

import java.util.function.Supplier;

public class Issue1633 {

    // supplyNullable is a supplier that may return null.
    // supplyNonNull is a supplier that does not return null.

    void foo1(Optional1633<String> o, Supplier<@Nullable String> supplyNullable) {
        // :: error: (argument.type.incompatible)
        @Nullable String str = o.orElseGetUnannotated(supplyNullable);
    }

    void foo2(Optional1633<String> o, Supplier<@Nullable String> supplyNullable) {
        @Nullable String str1 = o.orElseGetNullable(supplyNullable);
    }

    void foo2nw(Optional1633<String> o, Supplier<@Nullable String> supplyNullable) {
        @Nullable String str1 = o.orElseGetNullableNoWildcard(supplyNullable);
    }

    void foo3(Optional1633<String> o, Supplier<@Nullable String> supplyNullable) {
        // :: error: (argument.type.incompatible)
        @Nullable String str2 = o.orElseGetNonNull(supplyNullable);
    }

    void foo4(Optional1633<String> o, Supplier<@Nullable String> supplyNullable) {
        @Nullable String str3 = o.orElseGetPolyNull(supplyNullable);
    }

    void foo4nw(Optional1633<String> o, Supplier<@Nullable String> supplyNullable) {
        @Nullable String str3 = o.orElseGetPolyNullNoWildcard(supplyNullable);
    }

    void foo41(Optional1633<String> o) {
        @SuppressWarnings("return.type.incompatible") // https://tinyurl.com/cfissue/979
        @Nullable String str3 = o.orElseGetPolyNull(() -> null);
    }

    void foo41nw(Optional1633<String> o) {
        @SuppressWarnings("return.type.incompatible") // https://tinyurl.com/cfissue/979
        @Nullable String str3 = o.orElseGetPolyNullNoWildcard(() -> null);
    }

    void foo5(Optional1633<String> o, Supplier<@NonNull String> supplyNonNull) {
        @NonNull String str = o.orElseGetUnannotated(supplyNonNull);
    }

    void foo6(Optional1633<String> o, Supplier<@NonNull String> supplyNonNull) {
        // :: error: (assignment.type.incompatible)
        @NonNull String str1 = o.orElseGetNullable(supplyNonNull);
    }

    void foo6nw(Optional1633<String> o, Supplier<@NonNull String> supplyNonNull) {
        // :: error: (assignment.type.incompatible)
        @NonNull String str1 = o.orElseGetNullableNoWildcard(supplyNonNull);
    }

    void foo7(Optional1633<String> o, Supplier<@NonNull String> supplyNonNull) {
        @NonNull String str2 = o.orElseGetNonNull(supplyNonNull);
    }

    void foo8(Optional1633<String> o, Supplier<@NonNull String> supplyNonNull) {
        @NonNull String str3 = o.orElseGetPolyNull(supplyNonNull);
    }

    void foo8nw(Optional1633<String> o, Supplier<@NonNull String> supplyNonNull) {
        @NonNull String str3 = o.orElseGetPolyNullNoWildcard(supplyNonNull);
    }
}

// From the JDK
@Covariant(0)
final @NonNull class Optional1633<T extends @Nullable Object> {

    /** If non-null, the value; if null, indicates no value is present. */
    private final @Nullable T value = null;

    // TODO: there are conceptually two versions of this method:
    //   public @Nullable T orElseGet(Supplier<? extends @Nullable T> other) {
    //   public @NonNull T orElseGet(Supplier<? extends @NonNull T> other) {
    // Issue #1633 says that this annotation doesn't help at all:
    //   public @PolyNull T orElseGet(Supplier<? extends @PolyNull T> other) {
    // but it does seem to work in this test case.
    public T orElseGetUnannotated(Supplier<? extends T> other) {
        return value != null ? value : other.get();
    }

    public @Nullable T orElseGetNullable(Supplier<@Nullable ? extends @Nullable T> other) {
        return value != null ? value : other.get();
    }

    public @Nullable T orElseGetNullableNoWildcard(Supplier<? extends @Nullable T> other) {
        // The commented-out line fails to typecheck due to issue #979
        // return value != null ? value : other.get();
        if (value != null) {
            return value;
        } else {
            return other.get();
        }
    }

    public @NonNull T orElseGetNonNull(Supplier<@NonNull ? extends @NonNull T> other) {
        return value != null ? value : other.get();
    }

    public @PolyNull T orElseGetPolyNull(Supplier<@PolyNull ? extends @PolyNull T> other) {
        return value != null ? value : other.get();
    }

    public @PolyNull T orElseGetPolyNullNoWildcard(Supplier<? extends @PolyNull T> other) {
        // The commented-out line fails to typecheck due to issue #979
        // return value != null ? value : other.get();
        if (value != null) {
            return value;
        } else {
            return other.get();
        }
    }
}
