import org.checkerframework.checker.nullness.qual.Nullable;

public class NullableConstructor {

    // :: error: (nullness.on.constructor)
    @Nullable NullableConstructor() {}
}
