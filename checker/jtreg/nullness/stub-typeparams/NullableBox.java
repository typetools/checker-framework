import org.checkerframework.checker.nullness.qual.Nullable;

// This class is not compiled with the Nullness Checker,
// so that only explicit annotations are stored in bytecode.
public class NullableBox<T extends @Nullable Object> {
    static <S extends @Nullable Object> NullableBox<S> of(S in) {
        // Implementation doesn't matter.
        return null;
    }
}
