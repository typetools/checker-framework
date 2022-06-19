// Test case for eisop issue #204:
// https://github.com/eisop/checker-framework/issues/204
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

class MixTypeAndDeclAnno<T extends @Nullable Object> {
    @NonNull T t;
    @android.annotation.NonNull T tdecl;
    // :: error: (type.invalid.conflicting.annos)
    @android.annotation.NonNull @Nullable T tdecl2;
    // :: error: (type.invalid.conflicting.annos)
    @android.annotation.NonNull @android.annotation.Nullable Object f1;
    // :: error: (type.invalid.conflicting.annos)
    @android.annotation.NonNull @Nullable Object f2;
    // Nullable applies to the array itself, while NonNull apply to components of the array.
    @android.annotation.Nullable @NonNull Object[] g;
    // :: error: (type.invalid.conflicting.annos)
    @android.annotation.Nullable Object @NonNull [] k;

    MixTypeAndDeclAnno(
            @NonNull T t,
            @android.annotation.NonNull T tdecl,
            // :: error: (type.invalid.conflicting.annos)
            @android.annotation.NonNull @android.annotation.Nullable Object f1,
            // :: error: (type.invalid.conflicting.annos)
            @android.annotation.NonNull @Nullable Object f2,
            // :: error: (type.invalid.conflicting.annos)
            @android.annotation.Nullable Object @NonNull [] k) {
        this.t = t;
        this.tdecl = tdecl;
        // :: error: (type.invalid.conflicting.annos)
        this.f1 = f1;
        // :: error: (type.invalid.conflicting.annos)
        this.f2 = f2;
        // :: error: (type.invalid.conflicting.annos)
        this.k = k;
    }
}
