import java.util.List;
import org.checkerframework.checker.nullness.qual.*;

class WildcardAnnos {
    // :: error: (bound.type.incompatible)
    @Nullable List<@Nullable ? extends @NonNull Object> l1 = null;
    @Nullable List<@NonNull ? extends @Nullable Object> l2 = null;

    // The implicit upper bound is Nullable, because the annotation
    // on the wildcard is propagated. Therefore this type is:
    // @Nullable List<? super @NonNull Object extends @Nullable Object> l3 = null;
    @Nullable List<@Nullable ? super @NonNull Object> l3 = null;

    // :: error: (bound.type.incompatible)
    @Nullable List<@NonNull ? super @Nullable Object> l4 = null;

    @Nullable List<? super @Nullable Object> l5 = null;

    @Nullable List<? extends @Nullable Object> inReturn() {
        return null;
    }

    void asParam(List<? extends @Nullable Object> p) {}

    // :: error: (type.invalid.conflicting.annos)
    @Nullable List<@Nullable @NonNull ? extends @Nullable Object> l6 = null;
    // :: error: (type.invalid.conflicting.annos)
    @Nullable List<@Nullable @NonNull ? super @NonNull Object> l7 = null;
    // :: error: (type.invalid.conflicting.annos)
    @Nullable List<? extends @Nullable @NonNull Object> l8 = null;
    // :: error: (type.invalid.conflicting.annos)
    @Nullable List<? super @Nullable @NonNull Object> l9 = null;
}
