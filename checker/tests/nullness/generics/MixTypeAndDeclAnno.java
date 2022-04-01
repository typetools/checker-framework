// Test case for eisop issue #204:
// https://github.com/eisop/checker-framework/issues/204
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

class MixTypeAndDeclAnno<T extends @Nullable Object> {
    @NonNull T t;
    @android.annotation.NonNull T tdecl;

    MixTypeAndDeclAnno(@NonNull T t, @android.annotation.NonNull T tdecl) {
        this.t = t;
        this.tdecl = tdecl;
    }
}
