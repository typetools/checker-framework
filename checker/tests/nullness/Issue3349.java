import java.io.Serializable;
import org.checkerframework.checker.nullness.qual.*;

// TODO: warning about @Nullable see Issue 3362.
class Issue3348<T extends @NonNull Object & @Nullable Serializable> {
    void foo(T p1) {
        @NonNull Serializable s = p1;
        @NonNull Object o = p1;
    }
}
